#import "FilamentBridge.h"

#import <filament/Engine.h>
#import <filament/Renderer.h>
#import <filament/Scene.h>
#import <filament/View.h>
#import <filament/Camera.h>
#import <filament/Viewport.h>
#import <filament/SwapChain.h>
#import <filament/Material.h>
#import <filament/MaterialInstance.h>
#import <filament/LightManager.h>
#import <filament/RenderableManager.h>
#import <filament/VertexBuffer.h>
#import <filament/IndexBuffer.h>
#import <filament/Texture.h>
#import <filament/TextureSampler.h>
#import <utils/EntityManager.h>
#import <math/mat3.h>
#import <math/vec3.h>
#import <math/vec4.h>
#import <math/mat4.h>
#import <math/norm.h>

#include <cmath>
#include <cstddef>
#include <map>
#include <string>
#include <vector>

using namespace filament;
using namespace filament::backend;
using namespace utils;
using namespace math;

static NSData *PFLoadDataFromPathOrUri(NSString *location) {
    if (location.length == 0) return nil;

    NSData *data = [NSData dataWithContentsOfFile:location];
    if (data) return data;

    NSURL *url = [NSURL URLWithString:location];
    if (url) {
        data = [NSData dataWithContentsOfURL:url];
        if (data) return data;
    }

    if ([location hasPrefix:@"file://"]) {
        NSString *filePath = [location stringByRemovingPercentEncoding];
        filePath = [filePath stringByReplacingOccurrencesOfString:@"file://" withString:@""];
        data = [NSData dataWithContentsOfFile:filePath];
        if (data) return data;
    }

    return nil;
}

static void *PFMakeOwnedCopy(const void *source, size_t size) {
    void *copy = malloc(size);
    memcpy(copy, source, size);
    return copy;
}

static NSString *PFMaterialParameterTypeName(Material::ParameterInfo const& parameter) {
    if (parameter.isSampler) {
        switch (parameter.samplerType) {
            case SamplerType::SAMPLER_2D: return @"SAMPLER_2D";
            case SamplerType::SAMPLER_2D_ARRAY: return @"SAMPLER_2D_ARRAY";
            case SamplerType::SAMPLER_EXTERNAL: return @"SAMPLER_EXTERNAL";
            case SamplerType::SAMPLER_CUBEMAP: return @"SAMPLER_CUBEMAP";
            case SamplerType::SAMPLER_3D: return @"SAMPLER_3D";
            case SamplerType::SAMPLER_CUBEMAP_ARRAY: return @"SAMPLER_CUBEMAP_ARRAY";
            default: return @"UNKNOWN";
        }
    }
    switch (parameter.type) {
        case UniformType::BOOL: return @"BOOL";
        case UniformType::BOOL2: return @"BOOL2";
        case UniformType::BOOL3: return @"BOOL3";
        case UniformType::BOOL4: return @"BOOL4";
        case UniformType::FLOAT: return @"FLOAT";
        case UniformType::FLOAT2: return @"FLOAT2";
        case UniformType::FLOAT3: return @"FLOAT3";
        case UniformType::FLOAT4: return @"FLOAT4";
        case UniformType::INT: return @"INT";
        case UniformType::INT2: return @"INT2";
        case UniformType::INT3: return @"INT3";
        case UniformType::INT4: return @"INT4";
        case UniformType::UINT: return @"UINT";
        case UniformType::UINT2: return @"UINT2";
        case UniformType::UINT3: return @"UINT3";
        case UniformType::UINT4: return @"UINT4";
        case UniformType::MAT3: return @"MAT3";
        case UniformType::MAT4: return @"MAT4";
        default: return @"UNKNOWN";
    }
}

static NSString *PFMaterialParameterPrecisionName(Material::ParameterInfo const& parameter) {
    switch (parameter.precision) {
        case Precision::LOW: return @"LOW";
        case Precision::MEDIUM: return @"MEDIUM";
        case Precision::HIGH: return @"HIGH";
        case Precision::DEFAULT: return @"DEFAULT";
    }
}

@implementation FilamentBridge {
    Engine *_engine;
    Renderer *_renderer;
    Scene *_scene;
    View *_view;
    Camera *_camera;
    SwapChain *_swapChain;
    Entity _cameraEntity;

    std::map<int, Entity> _lights;
    std::map<int, Entity> _renderables;
    std::map<int, Material *> _materials;
    std::map<int, MaterialInstance *> _materialInstances;
    std::map<int, Texture *> _textures;
    std::vector<VertexBuffer *> _vertexBuffers;
    std::vector<IndexBuffer *> _indexBuffers;

    int _nextHandle;
    int _viewWidth;
    int _viewHeight;
}

- (void)initializeWithMetalLayer:(CAMetalLayer *)layer
                           width:(int)width
                          height:(int)height {
    _nextHandle = 1;
    _viewWidth = width;
    _viewHeight = height;

    _engine = Engine::create(Engine::Backend::METAL);
    _renderer = _engine->createRenderer();
    _renderer->setClearOptions({
        .clearColor = {0.12f, 0.12f, 0.14f, 1.0f},
        .clear = true
    });
    _scene = _engine->createScene();
    _view = _engine->createView();
    _view->setScene(_scene);

    _cameraEntity = EntityManager::get().create();
    _camera = _engine->createCamera(_cameraEntity);
    _view->setCamera(_camera);
    _view->setViewport({0, 0, (uint32_t)width, (uint32_t)height});

    _swapChain = _engine->createSwapChain((__bridge void *)layer);
}

- (void)destroy {
    if (!_engine) return;

    for (auto &pair : _renderables) {
        _scene->remove(pair.second);
        _engine->destroy(pair.second);
        EntityManager::get().destroy(pair.second);
    }
    _renderables.clear();

    for (auto &pair : _lights) {
        _scene->remove(pair.second);
        _engine->destroy(pair.second);
        EntityManager::get().destroy(pair.second);
    }
    _lights.clear();

    for (auto &pair : _materialInstances) {
        _engine->destroy(pair.second);
    }
    _materialInstances.clear();

    for (auto &pair : _materials) {
        _engine->destroy(pair.second);
    }
    _materials.clear();

    for (auto &pair : _textures) {
        _engine->destroy(pair.second);
    }
    _textures.clear();

    for (auto *vb : _vertexBuffers) _engine->destroy(vb);
    _vertexBuffers.clear();
    for (auto *ib : _indexBuffers) _engine->destroy(ib);
    _indexBuffers.clear();

    _engine->destroy(_view);
    _engine->destroy(_scene);
    _engine->destroyCameraComponent(_cameraEntity);
    _engine->destroy(_renderer);
    if (_swapChain) _engine->destroy(_swapChain);

    Engine::destroy(&_engine);
    _engine = nullptr;
}

- (void)clearScene {
    if (!_engine) return;
    for (auto &pair : _renderables) {
        _scene->remove(pair.second);
        _engine->destroy(pair.second);
        EntityManager::get().destroy(pair.second);
    }
    _renderables.clear();
}

- (void)updateCameraEyeX:(float)eyeX eyeY:(float)eyeY eyeZ:(float)eyeZ
                  targetX:(float)targetX targetY:(float)targetY targetZ:(float)targetZ
                      upX:(float)upX upY:(float)upY upZ:(float)upZ
                      fov:(double)fov near:(double)nearVal far:(double)farVal {
    if (!_camera) return;
    _camera->lookAt(
        {eyeX, eyeY, eyeZ},
        {targetX, targetY, targetZ},
        {upX, upY, upZ}
    );
    double aspect = (_viewWidth > 0 && _viewHeight > 0)
        ? (double)_viewWidth / (double)_viewHeight
        : 1.0;
    _camera->setProjection(fov, aspect, nearVal, farVal, Camera::Fov::VERTICAL);
}

- (int)addLightWithType:(int)type
                      r:(float)r g:(float)g b:(float)b
              intensity:(float)intensity
                   posX:(float)posX posY:(float)posY posZ:(float)posZ
                   dirX:(float)dirX dirY:(float)dirY dirZ:(float)dirZ
              innerCone:(float)innerCone outerCone:(float)outerCone {
    if (!_engine) return -1;

    Entity entity = EntityManager::get().create();

    LightManager::Type ltype;
    switch (type) {
        case 0: ltype = LightManager::Type::DIRECTIONAL; break;
        case 1: ltype = LightManager::Type::POINT; break;
        case 2: ltype = LightManager::Type::SPOT; break;
        default: ltype = LightManager::Type::DIRECTIONAL; break;
    }

    auto builder = LightManager::Builder(ltype)
        .color({r, g, b})
        .intensity(intensity)
        .position({posX, posY, posZ})
        .direction({dirX, dirY, dirZ});

    if (type == 2) {
        builder.spotLightCone(innerCone, outerCone);
    }

    builder.build(*_engine, entity);
    _scene->addEntity(entity);

    int handle = _nextHandle++;
    _lights[handle] = entity;
    return handle;
}

- (void)removeLight:(int)handle {
    auto it = _lights.find(handle);
    if (it == _lights.end() || !_engine) return;
    _scene->remove(it->second);
    _engine->destroy(it->second);
    EntityManager::get().destroy(it->second);
    _lights.erase(it);
}

- (void)clearLights {
    if (!_engine) return;
    for (auto &pair : _lights) {
        _scene->remove(pair.second);
        _engine->destroy(pair.second);
        EntityManager::get().destroy(pair.second);
    }
    _lights.clear();
}

- (int)loadMaterialFromPath:(NSString *)path {
    if (!_engine) return -1;
    NSData *data = PFLoadDataFromPathOrUri(path);
    if (!data) {
        NSLog(@"Failed to load material data from '%@'", path);
        return -1;
    }

    Material *mat = Material::Builder()
        .package(data.bytes, data.length)
        .build(*_engine);

    int handle = _nextHandle++;
    _materials[handle] = mat;
    return handle;
}

- (int)getMaterialParameterDefinitionCount:(int)materialHandle {
    auto it = _materials.find(materialHandle);
    if (it == _materials.end()) return 0;
    return (int)it->second->getParameterCount();
}

- (NSString *)getMaterialParameterName:(int)materialHandle index:(int)index {
    auto it = _materials.find(materialHandle);
    if (it == _materials.end()) return @"";
    size_t count = it->second->getParameterCount();
    if (index < 0 || (size_t)index >= count) return @"";
    std::vector<Material::ParameterInfo> params(count);
    it->second->getParameters(params.data(), count);
    return [NSString stringWithUTF8String:params[index].name];
}

- (NSString *)getMaterialParameterTypeName:(int)materialHandle index:(int)index {
    auto it = _materials.find(materialHandle);
    if (it == _materials.end()) return @"UNKNOWN";
    size_t count = it->second->getParameterCount();
    if (index < 0 || (size_t)index >= count) return @"UNKNOWN";
    std::vector<Material::ParameterInfo> params(count);
    it->second->getParameters(params.data(), count);
    return PFMaterialParameterTypeName(params[index]);
}

- (NSString *)getMaterialParameterPrecisionName:(int)materialHandle index:(int)index {
    auto it = _materials.find(materialHandle);
    if (it == _materials.end()) return @"DEFAULT";
    size_t count = it->second->getParameterCount();
    if (index < 0 || (size_t)index >= count) return @"DEFAULT";
    std::vector<Material::ParameterInfo> params(count);
    it->second->getParameters(params.data(), count);
    return PFMaterialParameterPrecisionName(params[index]);
}

- (int)getMaterialParameterArraySize:(int)materialHandle index:(int)index {
    auto it = _materials.find(materialHandle);
    if (it == _materials.end()) return 1;
    size_t count = it->second->getParameterCount();
    if (index < 0 || (size_t)index >= count) return 1;
    std::vector<Material::ParameterInfo> params(count);
    it->second->getParameters(params.data(), count);
    return (int)params[index].count;
}

- (int)createMaterialInstance:(int)materialHandle {
    auto it = _materials.find(materialHandle);
    if (it == _materials.end()) return -1;

    MaterialInstance *instance = it->second->createInstance();
    int handle = _nextHandle++;
    _materialInstances[handle] = instance;
    return handle;
}

- (void)setFloatParam:(int)instanceHandle name:(NSString *)name value:(float)value {
    auto it = _materialInstances.find(instanceHandle);
    if (it == _materialInstances.end()) return;
    it->second->setParameter(name.UTF8String, value);
}

- (void)setBoolParam:(int)instanceHandle name:(NSString *)name value:(BOOL)value {
    auto it = _materialInstances.find(instanceHandle);
    if (it == _materialInstances.end()) return;
    it->second->setParameter(name.UTF8String, (bool)value);
}

- (void)setIntParam:(int)instanceHandle name:(NSString *)name value:(int)value {
    auto it = _materialInstances.find(instanceHandle);
    if (it == _materialInstances.end()) return;
    it->second->setParameter(name.UTF8String, value);
}

- (void)setBool2Param:(int)instanceHandle name:(NSString *)name
                    x:(BOOL)x y:(BOOL)y {
    auto it = _materialInstances.find(instanceHandle);
    if (it == _materialInstances.end()) return;
    it->second->setParameter(name.UTF8String, bool2{(bool)x, (bool)y});
}

- (void)setBool3Param:(int)instanceHandle name:(NSString *)name
                    x:(BOOL)x y:(BOOL)y z:(BOOL)z {
    auto it = _materialInstances.find(instanceHandle);
    if (it == _materialInstances.end()) return;
    it->second->setParameter(name.UTF8String, bool3{(bool)x, (bool)y, (bool)z});
}

- (void)setBool4Param:(int)instanceHandle name:(NSString *)name
                    x:(BOOL)x y:(BOOL)y z:(BOOL)z w:(BOOL)w {
    auto it = _materialInstances.find(instanceHandle);
    if (it == _materialInstances.end()) return;
    it->second->setParameter(name.UTF8String, bool4{(bool)x, (bool)y, (bool)z, (bool)w});
}

- (void)setFloat2Param:(int)instanceHandle name:(NSString *)name
                     x:(float)x y:(float)y {
    auto it = _materialInstances.find(instanceHandle);
    if (it == _materialInstances.end()) return;
    it->second->setParameter(name.UTF8String, float2{x, y});
}

- (void)setFloat3Param:(int)instanceHandle name:(NSString *)name
                     x:(float)x y:(float)y z:(float)z {
    auto it = _materialInstances.find(instanceHandle);
    if (it == _materialInstances.end()) return;
    it->second->setParameter(name.UTF8String, float3{x, y, z});
}

- (void)setFloat4Param:(int)instanceHandle name:(NSString *)name
                     x:(float)x y:(float)y z:(float)z w:(float)w {
    auto it = _materialInstances.find(instanceHandle);
    if (it == _materialInstances.end()) return;
    it->second->setParameter(name.UTF8String, float4{x, y, z, w});
}

- (void)setInt2Param:(int)instanceHandle name:(NSString *)name
                   x:(int)x y:(int)y {
    auto it = _materialInstances.find(instanceHandle);
    if (it == _materialInstances.end()) return;
    it->second->setParameter(name.UTF8String, int2{x, y});
}

- (void)setInt3Param:(int)instanceHandle name:(NSString *)name
                   x:(int)x y:(int)y z:(int)z {
    auto it = _materialInstances.find(instanceHandle);
    if (it == _materialInstances.end()) return;
    it->second->setParameter(name.UTF8String, int3{x, y, z});
}

- (void)setInt4Param:(int)instanceHandle name:(NSString *)name
                   x:(int)x y:(int)y z:(int)z w:(int)w {
    auto it = _materialInstances.find(instanceHandle);
    if (it == _materialInstances.end()) return;
    it->second->setParameter(name.UTF8String, int4{x, y, z, w});
}

- (void)setMat3Param:(int)instanceHandle name:(NSString *)name
                   m00:(float)m00 m01:(float)m01 m02:(float)m02
                   m10:(float)m10 m11:(float)m11 m12:(float)m12
                   m20:(float)m20 m21:(float)m21 m22:(float)m22 {
    auto it = _materialInstances.find(instanceHandle);
    if (it == _materialInstances.end()) return;
    it->second->setParameter(
        name.UTF8String,
        mat3f{
            float3{m00, m01, m02},
            float3{m10, m11, m12},
            float3{m20, m21, m22},
        }
    );
}

- (void)setMat4Param:(int)instanceHandle name:(NSString *)name
                   m00:(float)m00 m01:(float)m01 m02:(float)m02 m03:(float)m03
                   m10:(float)m10 m11:(float)m11 m12:(float)m12 m13:(float)m13
                   m20:(float)m20 m21:(float)m21 m22:(float)m22 m23:(float)m23
                   m30:(float)m30 m31:(float)m31 m32:(float)m32 m33:(float)m33 {
    auto it = _materialInstances.find(instanceHandle);
    if (it == _materialInstances.end()) return;
    it->second->setParameter(
        name.UTF8String,
        mat4f{
            float4{m00, m01, m02, m03},
            float4{m10, m11, m12, m13},
            float4{m20, m21, m22, m23},
            float4{m30, m31, m32, m33},
        }
    );
}

- (int)createTextureWithWidth:(int)width height:(int)height pixels:(NSData *)pixels {
    if (!_engine) return -1;

    auto *texture = Texture::Builder()
        .width((uint32_t)width)
        .height((uint32_t)height)
        .sampler(Texture::Sampler::SAMPLER_2D)
        .format(Texture::InternalFormat::RGBA8)
        .levels(1)
        .build(*_engine);

    // Copy pixel data so it outlives the call
    size_t size = pixels.length;
    void *pixelCopy = malloc(size);
    memcpy(pixelCopy, pixels.bytes, size);

    Texture::PixelBufferDescriptor buffer(
        pixelCopy, size,
        Texture::Format::RGBA, Texture::Type::UBYTE,
        [](void *buf, size_t, void *) { free(buf); }
    );
    texture->setImage(*_engine, 0, std::move(buffer));

    int handle = _nextHandle++;
    _textures[handle] = texture;
    return handle;
}

- (void)setTextureParam:(int)instanceHandle name:(NSString *)name textureHandle:(int)textureHandle {
    auto instIt = _materialInstances.find(instanceHandle);
    if (instIt == _materialInstances.end()) return;
    auto texIt = _textures.find(textureHandle);
    if (texIt == _textures.end()) return;

    TextureSampler sampler(
        TextureSampler::MinFilter::LINEAR,
        TextureSampler::MagFilter::LINEAR,
        TextureSampler::WrapMode::REPEAT
    );
    instIt->second->setParameter(name.UTF8String, texIt->second, sampler);
}

- (int)createPlaneWithMaterial:(int)instanceHandle width:(float)width height:(float)height {
    if (!_engine) return -1;
    auto it = _materialInstances.find(instanceHandle);
    if (it == _materialInstances.end()) return -1;

    float hw = width / 2.0f;
    float hh = height / 2.0f;
    const short4 tangentFrame = packSnorm16(float4{0.0f, 0.0f, 0.0f, 1.0f});

    struct PlaneVertex {
        float px, py, pz;
        int16_t tx, ty, tz, tw;
        float u, v;
    };

    PlaneVertex vertices[] = {
        {-hw, -hh, 0.0f, tangentFrame.x, tangentFrame.y, tangentFrame.z, tangentFrame.w, 0.0f, 0.0f},
        { hw, -hh, 0.0f, tangentFrame.x, tangentFrame.y, tangentFrame.z, tangentFrame.w, 1.0f, 0.0f},
        { hw,  hh, 0.0f, tangentFrame.x, tangentFrame.y, tangentFrame.z, tangentFrame.w, 1.0f, 1.0f},
        {-hw,  hh, 0.0f, tangentFrame.x, tangentFrame.y, tangentFrame.z, tangentFrame.w, 0.0f, 1.0f},
    };
    uint16_t indices[] = {0, 1, 2, 0, 2, 3};

    auto *vb = VertexBuffer::Builder()
        .vertexCount(4)
        .bufferCount(1)
        .attribute(VertexAttribute::POSITION, 0, VertexBuffer::AttributeType::FLOAT3, 0, sizeof(PlaneVertex))
        .attribute(VertexAttribute::TANGENTS, 0, VertexBuffer::AttributeType::SHORT4, offsetof(PlaneVertex, tx), sizeof(PlaneVertex))
        .normalized(VertexAttribute::TANGENTS)
        .attribute(VertexAttribute::UV0, 0, VertexBuffer::AttributeType::FLOAT2, offsetof(PlaneVertex, u), sizeof(PlaneVertex))
        .build(*_engine);
    vb->setBufferAt(*_engine, 0, VertexBuffer::BufferDescriptor(
        PFMakeOwnedCopy(vertices, sizeof(vertices)),
        sizeof(vertices),
        [](void *buffer, size_t, void *) { free(buffer); }
    ));
    _vertexBuffers.push_back(vb);

    auto *ib = IndexBuffer::Builder()
        .indexCount(6)
        .bufferType(IndexBuffer::IndexType::USHORT)
        .build(*_engine);
    ib->setBuffer(*_engine, IndexBuffer::BufferDescriptor(
        PFMakeOwnedCopy(indices, sizeof(indices)),
        sizeof(indices),
        [](void *buffer, size_t, void *) { free(buffer); }
    ));
    _indexBuffers.push_back(ib);

    Entity entity = EntityManager::get().create();
    RenderableManager::Builder(1)
        .geometry(0, RenderableManager::PrimitiveType::TRIANGLES, vb, ib)
        .material(0, it->second)
        .boundingBox({{-hw, -hh, -0.01f}, {hw, hh, 0.01f}})
        .culling(false)
        .build(*_engine, entity);

    _scene->addEntity(entity);
    int handle = _nextHandle++;
    _renderables[handle] = entity;
    return handle;
}

- (int)createSphereWithMaterial:(int)instanceHandle radius:(float)radius {
    if (!_engine) return -1;
    auto it = _materialInstances.find(instanceHandle);
    if (it == _materialInstances.end()) return -1;

    const int stacks = 24;
    const int slices = 24;
    const int vertexCount = (stacks + 1) * (slices + 1);
    std::vector<float> vertexData(vertexCount * 8);
    int vi = 0;

    for (int i = 0; i <= stacks; i++) {
        float phi = M_PI * (float)i / stacks;
        float sinPhi = sinf(phi);
        float cosPhi = cosf(phi);
        for (int j = 0; j <= slices; j++) {
            float theta = 2.0f * M_PI * (float)j / slices;
            float sinTheta = sinf(theta);
            float cosTheta = cosf(theta);
            float x = cosTheta * sinPhi;
            float y = cosPhi;
            float z = sinTheta * sinPhi;
            vertexData[vi++] = x * radius;
            vertexData[vi++] = y * radius;
            vertexData[vi++] = z * radius;
            vertexData[vi++] = x;
            vertexData[vi++] = y;
            vertexData[vi++] = z;
            vertexData[vi++] = (float)j / slices;
            vertexData[vi++] = (float)i / stacks;
        }
    }

    int indexCount = stacks * slices * 6;
    std::vector<uint16_t> indexData(indexCount);
    int ii = 0;
    for (int i = 0; i < stacks; i++) {
        for (int j = 0; j < slices; j++) {
            int first = i * (slices + 1) + j;
            int second = first + slices + 1;
            indexData[ii++] = (uint16_t)first;
            indexData[ii++] = (uint16_t)second;
            indexData[ii++] = (uint16_t)(first + 1);
            indexData[ii++] = (uint16_t)second;
            indexData[ii++] = (uint16_t)(second + 1);
            indexData[ii++] = (uint16_t)(first + 1);
        }
    }

    auto *vb = VertexBuffer::Builder()
        .vertexCount(vertexCount)
        .bufferCount(1)
        .attribute(VertexAttribute::POSITION, 0, VertexBuffer::AttributeType::FLOAT3, 0, 32)
        .attribute(VertexAttribute::TANGENTS, 0, VertexBuffer::AttributeType::FLOAT3, 12, 32)
        .attribute(VertexAttribute::UV0, 0, VertexBuffer::AttributeType::FLOAT2, 24, 32)
        .build(*_engine);
    const size_t vertexDataSize = vertexData.size() * sizeof(float);
    vb->setBufferAt(*_engine, 0, VertexBuffer::BufferDescriptor(
        PFMakeOwnedCopy(vertexData.data(), vertexDataSize),
        vertexDataSize,
        [](void *buffer, size_t, void *) { free(buffer); }
    ));
    _vertexBuffers.push_back(vb);

    auto *ib = IndexBuffer::Builder()
        .indexCount(indexCount)
        .bufferType(IndexBuffer::IndexType::USHORT)
        .build(*_engine);
    const size_t indexDataSize = indexData.size() * sizeof(uint16_t);
    ib->setBuffer(*_engine, IndexBuffer::BufferDescriptor(
        PFMakeOwnedCopy(indexData.data(), indexDataSize),
        indexDataSize,
        [](void *buffer, size_t, void *) { free(buffer); }
    ));
    _indexBuffers.push_back(ib);

    Entity entity = EntityManager::get().create();
    RenderableManager::Builder(1)
        .geometry(0, RenderableManager::PrimitiveType::TRIANGLES, vb, ib)
        .material(0, it->second)
        .boundingBox({{-radius, -radius, -radius}, {radius, radius, radius}})
        .culling(false)
        .build(*_engine, entity);

    _scene->addEntity(entity);
    int handle = _nextHandle++;
    _renderables[handle] = entity;
    return handle;
}

- (void)removeRenderable:(int)handle {
    auto it = _renderables.find(handle);
    if (it == _renderables.end() || !_engine) return;
    _scene->remove(it->second);
    _engine->destroy(it->second);
    EntityManager::get().destroy(it->second);
    _renderables.erase(it);
}

- (void)render {
    if (!_renderer || !_swapChain || !_view) return;
    if (_renderer->beginFrame(_swapChain)) {
        _renderer->render(_view);
        _renderer->endFrame();
    }
}

- (void)updateViewportWidth:(int)width height:(int)height {
    _viewWidth = width;
    _viewHeight = height;
    if (_view) {
        _view->setViewport({0, 0, (uint32_t)width, (uint32_t)height});
    }
    if (_camera && width > 0 && height > 0) {
        double aspect = (double)width / (double)height;
        _camera->setProjection(45.0, aspect, 0.1, 100.0, Camera::Fov::VERTICAL);
    }
}

@end
