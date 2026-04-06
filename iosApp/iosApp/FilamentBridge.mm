#import "FilamentBridge.h"

#import <UIKit/UIKit.h>

#import <filament/Engine.h>
#import <filament/Renderer.h>
#import <filament/Scene.h>
#import <filament/View.h>
#import <filament/Camera.h>
#import <filament/Viewport.h>
#import <filament/SwapChain.h>
#import <filament/Material.h>
#import <filament/MaterialInstance.h>
#import <filament/IndirectLight.h>
#import <filament/LightManager.h>
#import <filament/MorphTargetBuffer.h>
#import <filament/RenderableManager.h>
#import <filament/TransformManager.h>
#import <filament/VertexBuffer.h>
#import <filament/IndexBuffer.h>
#import <filament/Skybox.h>
#import <filament/Texture.h>
#import <filament/TextureSampler.h>
#import <image/Ktx1Bundle.h>
#import <ktxreader/Ktx1Reader.h>
#import <utils/EntityManager.h>
#import <math/mat3.h>
#import <math/vec3.h>
#import <math/vec4.h>
#import <math/mat4.h>
#import <math/norm.h>
#import <geometry/SurfaceOrientation.h>

#include <cmath>
#include <algorithm>
#include <array>
#include <cstddef>
#include <cstring>
#include <limits>
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

typedef NS_ENUM(NSInteger, PFTextureColorFormat) {
    PFTextureColorFormatRgba8 = 0,
    PFTextureColorFormatSrgb8A8 = 1,
};

static uint8_t PFMipLevelCount(uint32_t width, uint32_t height) {
    uint32_t maxDimension = std::max(width, height);
    uint8_t levels = 1;
    while (maxDimension > 1) {
        maxDimension >>= 1;
        levels++;
    }
    return levels;
}

static Texture *PFCreateTextureFromRgbaBytes(
        Engine *engine,
        uint32_t width,
        uint32_t height,
        void *pixelData,
        size_t byteCount,
        PFTextureColorFormat colorFormat) {
    auto mipLevelCount = PFMipLevelCount(width, height);
    auto usage = Texture::Usage(Texture::Usage::DEFAULT | Texture::Usage::GEN_MIPMAPPABLE);
    auto *texture = Texture::Builder()
        .width(width)
        .height(height)
        .sampler(Texture::Sampler::SAMPLER_2D)
        .usage(usage)
        .format(
            colorFormat == PFTextureColorFormatSrgb8A8
                ? Texture::InternalFormat::SRGB8_A8
                : Texture::InternalFormat::RGBA8
        )
        .levels(mipLevelCount)
        .build(*engine);

    Texture::PixelBufferDescriptor buffer(
        pixelData, byteCount,
        Texture::Format::RGBA, Texture::Type::UBYTE,
        [](void *buffer, size_t, void *) { free(buffer); }
    );
    texture->setImage(*engine, 0, std::move(buffer));
    texture->generateMipmaps(*engine);
    return texture;
}

static std::vector<float3> PFFloat3VectorFromData(NSData *data) {
    const size_t floatCount = data.length / sizeof(float);
    std::vector<float3> values(floatCount / 3);
    auto *source = static_cast<float const *>(data.bytes);
    for (size_t index = 0; index < values.size(); index++) {
        values[index] = float3{
            source[index * 3],
            source[index * 3 + 1],
            source[index * 3 + 2],
        };
    }
    return values;
}

static std::vector<float2> PFFloat2VectorFromData(NSData *data) {
    const size_t floatCount = data.length / sizeof(float);
    std::vector<float2> values(floatCount / 2);
    auto *source = static_cast<float const *>(data.bytes);
    for (size_t index = 0; index < values.size(); index++) {
        values[index] = float2{
            source[index * 2],
            source[index * 2 + 1],
        };
    }
    return values;
}

static std::vector<ushort3> PFUShort3VectorFromData(NSData *data) {
    const size_t indexCount = data.length / sizeof(uint16_t);
    std::vector<ushort3> values(indexCount / 3);
    auto *source = static_cast<uint16_t const *>(data.bytes);
    for (size_t index = 0; index < values.size(); index++) {
        values[index] = ushort3{
            source[index * 3],
            source[index * 3 + 1],
            source[index * 3 + 2],
        };
    }
    return values;
}

static std::vector<uint16_t> PFUInt16VectorFromTriangles(std::vector<ushort3> const& triangles) {
    std::vector<uint16_t> values(triangles.size() * 3);
    for (size_t index = 0; index < triangles.size(); index++) {
        values[index * 3] = triangles[index].x;
        values[index * 3 + 1] = triangles[index].y;
        values[index * 3 + 2] = triangles[index].z;
    }
    return values;
}

static std::vector<uint16_t> PFUInt16VectorFromData(NSData *data) {
    const size_t indexCount = data.length / sizeof(uint16_t);
    std::vector<uint16_t> values(indexCount);
    if (indexCount > 0) {
        memcpy(values.data(), data.bytes, data.length);
    }
    return values;
}

static std::vector<int32_t> PFInt32VectorFromData(NSData *data) {
    const size_t valueCount = data.length / sizeof(int32_t);
    std::vector<int32_t> values(valueCount);
    if (valueCount > 0) {
        memcpy(values.data(), data.bytes, data.length);
    }
    return values;
}

static Box PFBoundsFromMorphData(
        std::vector<float3> const& positions,
        std::vector<std::vector<float3>> const& morphTargets) {
    float3 minBounds{
        std::numeric_limits<float>::infinity(),
        std::numeric_limits<float>::infinity(),
        std::numeric_limits<float>::infinity(),
    };
    float3 maxBounds{
        -std::numeric_limits<float>::infinity(),
        -std::numeric_limits<float>::infinity(),
        -std::numeric_limits<float>::infinity(),
    };

    auto include = [&](std::vector<float3> const& values) {
        for (auto const& value : values) {
            minBounds = min(minBounds, value);
            maxBounds = max(maxBounds, value);
        }
    };

    include(positions);
    for (auto const& target : morphTargets) {
        include(target);
    }
    return Box{minBounds, maxBounds};
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

static VertexAttribute PFVertexAttributeFromValue(int32_t value) {
    switch (value) {
        case 0: return VertexAttribute::POSITION;
        case 1: return VertexAttribute::TANGENTS;
        case 2: return VertexAttribute::UV0;
        case 3: return VertexAttribute::COLOR;
        default: return VertexAttribute::POSITION;
    }
}

static VertexBuffer::AttributeType PFAttributeTypeFromValue(int32_t value) {
    switch (value) {
        case 0: return VertexBuffer::AttributeType::FLOAT2;
        case 1: return VertexBuffer::AttributeType::FLOAT3;
        case 2: return VertexBuffer::AttributeType::FLOAT4;
        case 3: return VertexBuffer::AttributeType::UBYTE4;
        default: return VertexBuffer::AttributeType::FLOAT3;
    }
}

static RenderableManager::PrimitiveType PFPrimitiveTypeFromValue(int32_t value) {
    switch (value) {
        case 1: return RenderableManager::PrimitiveType::LINES;
        case 2: return RenderableManager::PrimitiveType::POINTS;
        default: return RenderableManager::PrimitiveType::TRIANGLES;
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
    std::map<int, IndirectLight *> _indirectLights;
    std::map<int, Skybox *> _skyboxes;
    std::map<int, Material *> _materials;
    std::map<int, MaterialInstance *> _materialInstances;
    std::map<int, Texture *> _textures;
    std::map<int, MorphTargetBuffer *> _morphTargetBuffers;
    std::vector<VertexBuffer *> _vertexBuffers;
    std::vector<IndexBuffer *> _indexBuffers;

    int _nextHandle;
    int _viewWidth;
    int _viewHeight;
    float4 _clearColor;
}

- (void)initializeWithMetalLayer:(CAMetalLayer *)layer
                           width:(int)width
                          height:(int)height
                        isOpaque:(BOOL)isOpaque {
    _nextHandle = 1;
    _viewWidth = width;
    _viewHeight = height;
    _clearColor = float4{0.0f, 0.0f, 0.0f, 1.0f};

    _engine = Engine::create(Engine::Backend::METAL);
    _renderer = _engine->createRenderer();
    _renderer->setClearOptions({
        .clearColor = _clearColor,
        .clear = true
    });
    _scene = _engine->createScene();
    _view = _engine->createView();
    _view->setScene(_scene);

    _cameraEntity = EntityManager::get().create();
    _camera = _engine->createCamera(_cameraEntity);
    _view->setCamera(_camera);
    _view->setViewport({0, 0, (uint32_t)width, (uint32_t)height});

    _swapChain = _engine->createSwapChain(
        (__bridge void *)layer,
        isOpaque ? 0 : SwapChain::CONFIG_TRANSPARENT
    );
}

- (void)setClearColorR:(float)r g:(float)g b:(float)b a:(float)a {
    _clearColor = float4{r, g, b, a};
    if (_renderer) {
        _renderer->setClearOptions({
            .clearColor = _clearColor,
            .clear = true
        });
    }
}

- (void)destroy {
    if (!_engine) return;

    _scene->setIndirectLight(nullptr);
    _scene->setSkybox(nullptr);

    for (auto &pair : _renderables) {
        _scene->remove(pair.second);
        _engine->destroy(pair.second);
        EntityManager::get().destroy(pair.second);
    }
    _renderables.clear();
    for (auto &pair : _morphTargetBuffers) {
        _engine->destroy(pair.second);
    }
    _morphTargetBuffers.clear();

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

    for (auto &pair : _indirectLights) {
        _engine->destroy(pair.second);
    }
    _indirectLights.clear();

    for (auto &pair : _skyboxes) {
        _engine->destroy(pair.second);
    }
    _skyboxes.clear();

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
    for (auto &pair : _morphTargetBuffers) {
        _engine->destroy(pair.second);
    }
    _morphTargetBuffers.clear();
}

- (void)updateCameraEyeX:(float)eyeX eyeY:(float)eyeY eyeZ:(float)eyeZ
                  targetX:(float)targetX targetY:(float)targetY targetZ:(float)targetZ
                      upX:(float)upX upY:(float)upY upZ:(float)upZ
                      fov:(double)fov near:(double)nearVal far:(double)farVal
            projectionType:(int)projectionType orthoZoom:(double)orthoZoom {
    if (!_camera) return;
    _camera->lookAt(
        {eyeX, eyeY, eyeZ},
        {targetX, targetY, targetZ},
        {upX, upY, upZ}
    );
    double aspect = (_viewWidth > 0 && _viewHeight > 0)
        ? (double)_viewWidth / (double)_viewHeight
        : 1.0;
    if (projectionType == 1) {
        const double orthoHeight = orthoZoom;
        const double orthoWidth = orthoHeight * aspect;
        _camera->setProjection(
            Camera::Projection::ORTHO,
            -orthoWidth,
            orthoWidth,
            -orthoHeight,
            orthoHeight,
            nearVal,
            farVal
        );
    } else {
        _camera->setProjection(fov, aspect, nearVal, farVal, Camera::Fov::VERTICAL);
    }
}

- (void)setCameraExposure:(float)aperture shutterSpeed:(float)shutterSpeed sensitivity:(float)sensitivity {
    if (!_camera) return;
    _camera->setExposure(aperture, shutterSpeed, sensitivity);
}

- (int)addLightWithType:(int)type
                      r:(float)r g:(float)g b:(float)b
              intensity:(float)intensity
                   posX:(float)posX posY:(float)posY posZ:(float)posZ
            falloffRadius:(float)falloffRadius
                   dirX:(float)dirX dirY:(float)dirY dirZ:(float)dirZ
              innerCone:(float)innerCone outerCone:(float)outerCone
      sunAngularRadius:(float)sunAngularRadius
            sunHaloSize:(float)sunHaloSize
         sunHaloFalloff:(float)sunHaloFalloff {
    if (!_engine) return -1;

    Entity entity = EntityManager::get().create();

    LightManager::Type ltype;
    switch (type) {
        case 0: ltype = LightManager::Type::DIRECTIONAL; break;
        case 1: ltype = LightManager::Type::POINT; break;
        case 2: ltype = LightManager::Type::SPOT; break;
        case 3: ltype = LightManager::Type::SUN; break;
        default: ltype = LightManager::Type::DIRECTIONAL; break;
    }

    auto builder = LightManager::Builder(ltype)
        .color({r, g, b})
        .intensity(intensity)
        .position({posX, posY, posZ})
        .direction({dirX, dirY, dirZ});

    if (type == 1 || type == 2) {
        builder.falloff(falloffRadius);
    }
    if (type == 2) {
        builder.spotLightCone(innerCone, outerCone);
    }
    if (type == 3) {
        builder.sunAngularRadius(sunAngularRadius);
        builder.sunHaloSize(sunHaloSize);
        builder.sunHaloFalloff(sunHaloFalloff);
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

- (int)loadIndirectLightFromPath:(NSString *)path {
    if (!_engine) return -1;
    NSData *data = PFLoadDataFromPathOrUri(path);
    if (!data) {
        NSLog(@"Failed to load indirect light data from '%@'", path);
        return -1;
    }

    auto ktx = std::make_unique<image::Ktx1Bundle>(
        static_cast<const uint8_t *>(data.bytes),
        (uint32_t)data.length
    );
    filament::math::float3 sphericalHarmonics[9];
    if (!ktx->getSphericalHarmonics(sphericalHarmonics)) {
        NSLog(@"Failed to read spherical harmonics from '%@'", path);
        return -1;
    }

    Texture *cubemap = ktxreader::Ktx1Reader::createTexture(_engine, ktx.release(), false);
    if (!cubemap) {
        NSLog(@"Failed to create indirect light cubemap from '%@'", path);
        return -1;
    }

    IndirectLight *indirectLight = IndirectLight::Builder()
        .reflections(cubemap)
        .irradiance(3, sphericalHarmonics)
        .build(*_engine);
    if (!indirectLight) {
        NSLog(@"Failed to build indirect light from '%@'", path);
        _engine->destroy(cubemap);
        return -1;
    }

    int textureHandle = _nextHandle++;
    _textures[textureHandle] = cubemap;

    int handle = _nextHandle++;
    _indirectLights[handle] = indirectLight;
    return handle;
}

- (void)setIndirectLight:(int)handle intensity:(float)intensity {
    auto it = _indirectLights.find(handle);
    if (it == _indirectLights.end() || !_scene) return;
    it->second->setIntensity(intensity);
    _scene->setIndirectLight(it->second);
}

- (int)loadSkyboxFromPath:(NSString *)path {
    if (!_engine) return -1;
    NSData *data = PFLoadDataFromPathOrUri(path);
    if (!data) {
        NSLog(@"Failed to load skybox data from '%@'", path);
        return -1;
    }

    auto ktx = std::make_unique<image::Ktx1Bundle>(
        static_cast<const uint8_t *>(data.bytes),
        (uint32_t)data.length
    );
    Texture *cubemap = ktxreader::Ktx1Reader::createTexture(_engine, ktx.release(), false);
    if (!cubemap) {
        NSLog(@"Failed to create skybox cubemap from '%@'", path);
        return -1;
    }

    Skybox *skybox = Skybox::Builder()
        .environment(cubemap)
        .build(*_engine);
    if (!skybox) {
        NSLog(@"Failed to build skybox from '%@'", path);
        _engine->destroy(cubemap);
        return -1;
    }

    int textureHandle = _nextHandle++;
    _textures[textureHandle] = cubemap;

    int handle = _nextHandle++;
    _skyboxes[handle] = skybox;
    return handle;
}

- (int)createColorSkybox {
    if (!_engine) return -1;

    Skybox *skybox = Skybox::Builder().build(*_engine);
    if (!skybox) {
        return -1;
    }

    int handle = _nextHandle++;
    _skyboxes[handle] = skybox;
    return handle;
}

- (void)setSkybox:(int)handle {
    auto it = _skyboxes.find(handle);
    if (it == _skyboxes.end() || !_scene) return;
    _scene->setSkybox(it->second);
}

- (void)setSkyboxColorHandle:(int)handle r:(float)r g:(float)g b:(float)b a:(float)a {
    auto it = _skyboxes.find(handle);
    if (it == _skyboxes.end()) return;
    it->second->setColor({r, g, b, a});
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

    // Copy pixel data so it outlives the call
    size_t size = pixels.length;
    void *pixelCopy = malloc(size);
    memcpy(pixelCopy, pixels.bytes, size);

    auto *texture = PFCreateTextureFromRgbaBytes(
        _engine,
        (uint32_t)width,
        (uint32_t)height,
        pixelCopy,
        size,
        PFTextureColorFormatRgba8
    );

    int handle = _nextHandle++;
    _textures[handle] = texture;
    return handle;
}

- (int)loadTextureFromPath:(NSString *)path colorFormat:(int)colorFormat {
    if (!_engine) return -1;

    NSData *data = PFLoadDataFromPathOrUri(path);
    if (!data) {
        NSLog(@"Failed to load texture data from '%@'", path);
        return -1;
    }

    UIImage *image = [UIImage imageWithData:data];
    CGImageRef cgImage = image.CGImage;
    if (!cgImage) {
        NSLog(@"Failed to decode texture image from '%@'", path);
        return -1;
    }

    const size_t width = CGImageGetWidth(cgImage);
    const size_t height = CGImageGetHeight(cgImage);
    const size_t bytesPerRow = width * 4;
    const size_t byteCount = bytesPerRow * height;
    void *pixelCopy = calloc(1, byteCount);
    if (!pixelCopy) {
        return -1;
    }

    CGColorSpaceRef colorSpace = CGColorSpaceCreateDeviceRGB();
    CGContextRef context = CGBitmapContextCreate(
        pixelCopy,
        width,
        height,
        8,
        bytesPerRow,
        colorSpace,
        kCGImageAlphaPremultipliedLast | kCGBitmapByteOrder32Big
    );
    CGColorSpaceRelease(colorSpace);

    if (!context) {
        free(pixelCopy);
        NSLog(@"Failed to create texture bitmap context for '%@'", path);
        return -1;
    }

    CGContextDrawImage(context, CGRectMake(0, 0, width, height), cgImage);
    CGContextRelease(context);

    auto *texture = PFCreateTextureFromRgbaBytes(
        _engine,
        (uint32_t)width,
        (uint32_t)height,
        pixelCopy,
        byteCount,
        static_cast<PFTextureColorFormat>(colorFormat)
    );

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
        TextureSampler::MinFilter::LINEAR_MIPMAP_LINEAR,
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

    const int stacks = 64;
    const int slices = 64;
    const int vertexCount = (stacks + 1) * (slices + 1);

    // Generate positions, normals, and UVs
    std::vector<filament::math::float3> positions(vertexCount);
    std::vector<filament::math::float3> normals(vertexCount);
    std::vector<filament::math::float2> uvs(vertexCount);
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
            positions[vi] = {x * radius, y * radius, z * radius};
            normals[vi] = {x, y, z};
            uvs[vi] = {(float)j / slices, (float)i / stacks};
            vi++;
        }
    }

    // Generate indices
    int indexCount = stacks * slices * 6;
    std::vector<filament::math::ushort3> triangles(stacks * slices * 2);
    int ti = 0;
    for (int i = 0; i < stacks; i++) {
        for (int j = 0; j < slices; j++) {
            uint16_t first = (uint16_t)(i * (slices + 1) + j);
            uint16_t second = first + slices + 1;
            triangles[ti++] = {first, (uint16_t)(first + 1), second};
            triangles[ti++] = {(uint16_t)(first + 1), (uint16_t)(second + 1), second};
        }
    }

    // Use Filament's SurfaceOrientation to compute tangent quaternions
    auto* orientation = filament::geometry::SurfaceOrientation::Builder()
        .vertexCount((size_t)vertexCount)
        .normals(normals.data())
        .positions(positions.data())
        .uvs(uvs.data())
        .triangleCount((size_t)(stacks * slices * 2))
        .triangles(triangles.data())
        .build();

    std::vector<filament::math::short4> tangentQuats(vertexCount);
    orientation->getQuats(tangentQuats.data(), (size_t)vertexCount);
    delete orientation;

    // Build interleaved vertex data: position(3 floats) + tangent(4 shorts) + uv(2 floats)
    struct SphereVertex {
        float px, py, pz;
        int16_t tx, ty, tz, tw;
        float u, v;
    };
    static_assert(sizeof(SphereVertex) == 28, "SphereVertex must be 28 bytes");

    std::vector<SphereVertex> vertexData(vertexCount);
    for (int k = 0; k < vertexCount; k++) {
        vertexData[k] = {
            positions[k].x, positions[k].y, positions[k].z,
            tangentQuats[k].x, tangentQuats[k].y, tangentQuats[k].z, tangentQuats[k].w,
            uvs[k].x, uvs[k].y,
        };
    }

    // Build index buffer as flat uint16_t array
    std::vector<uint16_t> indexData(indexCount);
    for (int k = 0; k < (int)triangles.size(); k++) {
        indexData[k * 3 + 0] = triangles[k].x;
        indexData[k * 3 + 1] = triangles[k].y;
        indexData[k * 3 + 2] = triangles[k].z;
    }

    auto *vb = VertexBuffer::Builder()
        .vertexCount(vertexCount)
        .bufferCount(1)
        .attribute(VertexAttribute::POSITION, 0, VertexBuffer::AttributeType::FLOAT3, offsetof(SphereVertex, px), sizeof(SphereVertex))
        .attribute(VertexAttribute::TANGENTS, 0, VertexBuffer::AttributeType::SHORT4, offsetof(SphereVertex, tx), sizeof(SphereVertex))
        .normalized(VertexAttribute::TANGENTS)
        .attribute(VertexAttribute::UV0, 0, VertexBuffer::AttributeType::FLOAT2, offsetof(SphereVertex, u), sizeof(SphereVertex))
        .build(*_engine);
    const size_t vertexDataSize = vertexData.size() * sizeof(SphereVertex);
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

- (int)createCustomRenderableWithMaterial:(int)instanceHandle
                               vertexData:(NSData *)vertexDataData
                              vertexCount:(int)vertexCount
                              strideBytes:(int)strideBytes
                               attributes:(NSData *)attributesData
                                  indices:(NSData *)indicesData
                            primitiveType:(int)primitiveType
                                  centerX:(float)centerX centerY:(float)centerY centerZ:(float)centerZ
                              halfExtentX:(float)halfExtentX halfExtentY:(float)halfExtentY halfExtentZ:(float)halfExtentZ {
    if (!_engine) return -1;
    auto it = _materialInstances.find(instanceHandle);
    if (it == _materialInstances.end()) return -1;
    if (vertexCount <= 0 || strideBytes <= 0 || vertexDataData.length == 0 || indicesData.length == 0) {
        return -1;
    }

    const auto rawAttributes = PFInt32VectorFromData(attributesData);
    if (rawAttributes.empty() || rawAttributes.size() % 4 != 0) {
        return -1;
    }

    struct AttributeSpec {
        VertexAttribute attribute;
        VertexBuffer::AttributeType type;
        uint32_t offset;
        bool normalized;
    };

    std::vector<AttributeSpec> attributeSpecs;
    attributeSpecs.reserve(rawAttributes.size() / 4);
    bool hasPositionFloat3 = false;
    bool hasUvFloat2 = false;
    bool hasTangents = false;
    uint32_t positionOffset = 0;
    uint32_t uvOffset = 0;

    for (size_t index = 0; index < rawAttributes.size(); index += 4) {
        const auto attribute = PFVertexAttributeFromValue(rawAttributes[index]);
        const auto type = PFAttributeTypeFromValue(rawAttributes[index + 1]);
        const uint32_t offset = (uint32_t)rawAttributes[index + 2];
        const bool normalized = rawAttributes[index + 3] != 0;
        attributeSpecs.push_back({attribute, type, offset, normalized});
        if (attribute == VertexAttribute::POSITION && type == VertexBuffer::AttributeType::FLOAT3) {
            hasPositionFloat3 = true;
            positionOffset = offset;
        }
        if (attribute == VertexAttribute::UV0 && type == VertexBuffer::AttributeType::FLOAT2) {
            hasUvFloat2 = true;
            uvOffset = offset;
        }
        if (attribute == VertexAttribute::TANGENTS) {
            hasTangents = true;
        }
    }

    const auto indexData = PFUInt16VectorFromData(indicesData);
    if (indexData.empty()) {
        return -1;
    }

    VertexBuffer *vb = nullptr;

    if (primitiveType == 0 && hasPositionFloat3 && hasUvFloat2 && !hasTangents) {
        const auto triangles = PFUShort3VectorFromData(indicesData);
        if (triangles.empty()) {
            return -1;
        }

        std::vector<float3> positions((size_t)vertexCount);
        std::vector<float2> uvs((size_t)vertexCount);
        auto *source = static_cast<uint8_t const *>(vertexDataData.bytes);
        for (int vertexIndex = 0; vertexIndex < vertexCount; vertexIndex++) {
            const uint8_t *vertexBase = source + ((size_t)vertexIndex * (size_t)strideBytes);
            memcpy(&positions[(size_t)vertexIndex], vertexBase + positionOffset, sizeof(float3));
            memcpy(&uvs[(size_t)vertexIndex], vertexBase + uvOffset, sizeof(float2));
        }

        auto* orientation = filament::geometry::SurfaceOrientation::Builder()
            .vertexCount((size_t)vertexCount)
            .positions(positions.data())
            .uvs(uvs.data())
            .triangleCount(triangles.size())
            .triangles(triangles.data())
            .build();
        std::vector<short4> tangentQuats((size_t)vertexCount);
        orientation->getQuats(tangentQuats.data(), (size_t)vertexCount);
        delete orientation;

        struct GeneratedVertex {
            float px, py, pz;
            int16_t tx, ty, tz, tw;
            float u, v;
        };
        static_assert(sizeof(GeneratedVertex) == 28, "GeneratedVertex must be 28 bytes");

        std::vector<GeneratedVertex> generatedVertices((size_t)vertexCount);
        for (int vertexIndex = 0; vertexIndex < vertexCount; vertexIndex++) {
            generatedVertices[(size_t)vertexIndex] = {
                positions[(size_t)vertexIndex].x, positions[(size_t)vertexIndex].y, positions[(size_t)vertexIndex].z,
                tangentQuats[(size_t)vertexIndex].x, tangentQuats[(size_t)vertexIndex].y, tangentQuats[(size_t)vertexIndex].z, tangentQuats[(size_t)vertexIndex].w,
                uvs[(size_t)vertexIndex].x, uvs[(size_t)vertexIndex].y,
            };
        }

        vb = VertexBuffer::Builder()
            .vertexCount((uint32_t)vertexCount)
            .bufferCount(1)
            .attribute(VertexAttribute::POSITION, 0, VertexBuffer::AttributeType::FLOAT3, offsetof(GeneratedVertex, px), sizeof(GeneratedVertex))
            .attribute(VertexAttribute::TANGENTS, 0, VertexBuffer::AttributeType::SHORT4, offsetof(GeneratedVertex, tx), sizeof(GeneratedVertex))
            .normalized(VertexAttribute::TANGENTS)
            .attribute(VertexAttribute::UV0, 0, VertexBuffer::AttributeType::FLOAT2, offsetof(GeneratedVertex, u), sizeof(GeneratedVertex))
            .build(*_engine);
        const size_t generatedVertexDataSize = generatedVertices.size() * sizeof(GeneratedVertex);
        vb->setBufferAt(*_engine, 0, VertexBuffer::BufferDescriptor(
            PFMakeOwnedCopy(generatedVertices.data(), generatedVertexDataSize),
            generatedVertexDataSize,
            [](void *buffer, size_t, void *) { free(buffer); }
        ));
    } else {
        auto builder = VertexBuffer::Builder();
        builder.vertexCount((uint32_t)vertexCount).bufferCount(1);
        for (auto const& spec : attributeSpecs) {
            builder.attribute(spec.attribute, 0, spec.type, spec.offset, (uint8_t)strideBytes);
            if (spec.normalized) {
                builder.normalized(spec.attribute);
            }
        }
        vb = builder.build(*_engine);
        vb->setBufferAt(*_engine, 0, VertexBuffer::BufferDescriptor(
            PFMakeOwnedCopy(vertexDataData.bytes, vertexDataData.length),
            vertexDataData.length,
            [](void *buffer, size_t, void *) { free(buffer); }
        ));
    }
    _vertexBuffers.push_back(vb);

    auto *ib = IndexBuffer::Builder()
        .indexCount((uint32_t)indexData.size())
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
        .geometry(0, PFPrimitiveTypeFromValue(primitiveType), vb, ib)
        .material(0, it->second)
        .boundingBox(Box(float3{centerX, centerY, centerZ}, float3{halfExtentX, halfExtentY, halfExtentZ}))
        .build(*_engine, entity);

    _scene->addEntity(entity);
    int handle = _nextHandle++;
    _renderables[handle] = entity;
    return handle;
}

- (int)createCustomRenderableWithVertexData:(NSData *)vertexData
                                vertexCount:(int)vertexCount
                                strideBytes:(int)strideBytes
                             attributeKinds:(NSArray<NSNumber *> *)attributeKinds
                             attributeTypes:(NSArray<NSNumber *> *)attributeTypes
                           attributeOffsets:(NSArray<NSNumber *> *)attributeOffsets
                        attributeNormalized:(NSArray<NSNumber *> *)attributeNormalized
                                     indices:(NSData *)indicesData
                     materialInstanceHandle:(int)materialInstanceHandle
                                     bboxCX:(float)bboxCX bboxCY:(float)bboxCY bboxCZ:(float)bboxCZ
                                     bboxHX:(float)bboxHX bboxHY:(float)bboxHY bboxHZ:(float)bboxHZ
                              primitiveType:(int)primitiveType {
    if (!_engine) return -1;
    auto materialIt = _materialInstances.find(materialInstanceHandle);
    if (materialIt == _materialInstances.end()) return -1;

    const NSUInteger attributeCount = attributeKinds.count;
    if (attributeCount == 0 ||
        attributeTypes.count != attributeCount ||
        attributeOffsets.count != attributeCount ||
        attributeNormalized.count != attributeCount) {
        return -1;
    }
    if (vertexCount <= 0 || strideBytes <= 0 || vertexData.length == 0 || indicesData.length == 0) {
        return -1;
    }

    VertexBuffer::Builder builder;
    builder
        .vertexCount((uint32_t)vertexCount)
        .bufferCount(1);
    for (NSUInteger index = 0; index < attributeCount; index++) {
        const VertexAttribute attribute = PFVertexAttributeFromValue((int32_t)attributeKinds[index].intValue);
        builder.attribute(
            attribute,
            0,
            PFAttributeTypeFromValue((int32_t)attributeTypes[index].intValue),
            (uint32_t)attributeOffsets[index].intValue,
            (uint32_t)strideBytes
        );
        if (attributeNormalized[index].boolValue) {
            builder.normalized(attribute);
        }
    }

    auto *vb = builder.build(*_engine);
    vb->setBufferAt(*_engine, 0, VertexBuffer::BufferDescriptor(
        PFMakeOwnedCopy(vertexData.bytes, vertexData.length),
        vertexData.length,
        [](void *buffer, size_t, void *) { free(buffer); }
    ));
    _vertexBuffers.push_back(vb);

    auto *ib = IndexBuffer::Builder()
        .indexCount((uint32_t)(indicesData.length / sizeof(uint16_t)))
        .bufferType(IndexBuffer::IndexType::USHORT)
        .build(*_engine);
    ib->setBuffer(*_engine, IndexBuffer::BufferDescriptor(
        PFMakeOwnedCopy(indicesData.bytes, indicesData.length),
        indicesData.length,
        [](void *buffer, size_t, void *) { free(buffer); }
    ));
    _indexBuffers.push_back(ib);

    Entity entity = EntityManager::get().create();
    RenderableManager::Builder(1)
        .geometry(0, PFPrimitiveTypeFromValue(primitiveType), vb, ib)
        .material(0, materialIt->second)
        .boundingBox(Box(float3{bboxCX, bboxCY, bboxCZ}, float3{bboxHX, bboxHY, bboxHZ}))
        .build(*_engine, entity);

    _scene->addEntity(entity);
    int handle = _nextHandle++;
    _renderables[handle] = entity;
    return handle;
}

- (int)createMorphRenderableWithMaterial:(int)instanceHandle
                               positions:(NSData *)positionsData
                                      uvs:(NSData *)uvsData
                                  indices:(NSData *)indicesData
                      morphTargetPositions:(NSData *)morphTargetPositionsData
                          morphTargetCount:(int)morphTargetCount {
    if (!_engine) return -1;
    auto it = _materialInstances.find(instanceHandle);
    if (it == _materialInstances.end()) return -1;

    const auto positions = PFFloat3VectorFromData(positionsData);
    const auto uvs = PFFloat2VectorFromData(uvsData);
    const auto triangles = PFUShort3VectorFromData(indicesData);
    if (positions.empty() || uvs.size() != positions.size() || triangles.empty() || morphTargetCount <= 0) {
        return -1;
    }

    const size_t morphFloatCount = morphTargetPositionsData.length / sizeof(float);
    const size_t valuesPerTarget = positions.size() * 3;
    if (morphFloatCount != valuesPerTarget * (size_t)morphTargetCount) {
        return -1;
    }
    auto *morphSource = static_cast<float const *>(morphTargetPositionsData.bytes);
    std::vector<std::vector<float3>> morphTargets((size_t)morphTargetCount);
    for (int targetIndex = 0; targetIndex < morphTargetCount; targetIndex++) {
        auto& target = morphTargets[(size_t)targetIndex];
        target.resize(positions.size());
        const size_t baseOffset = (size_t)targetIndex * valuesPerTarget;
        for (size_t vertexIndex = 0; vertexIndex < positions.size(); vertexIndex++) {
            const size_t offset = baseOffset + vertexIndex * 3;
            target[vertexIndex] = float3{
                morphSource[offset],
                morphSource[offset + 1],
                morphSource[offset + 2],
            };
        }
    }

    auto* baseOrientation = filament::geometry::SurfaceOrientation::Builder()
        .vertexCount(positions.size())
        .positions(positions.data())
        .uvs(uvs.data())
        .triangleCount(triangles.size())
        .triangles(triangles.data())
        .build();
    std::vector<short4> baseTangents(positions.size());
    baseOrientation->getQuats(baseTangents.data(), positions.size());
    delete baseOrientation;

    struct MorphRenderableVertex {
        float px, py, pz;
        int16_t tx, ty, tz, tw;
        float u, v;
    };
    static_assert(sizeof(MorphRenderableVertex) == 28, "MorphRenderableVertex must be 28 bytes");

    std::vector<MorphRenderableVertex> vertexData(positions.size());
    for (size_t index = 0; index < positions.size(); index++) {
        vertexData[index] = {
            positions[index].x, positions[index].y, positions[index].z,
            baseTangents[index].x, baseTangents[index].y, baseTangents[index].z, baseTangents[index].w,
            uvs[index].x, uvs[index].y,
        };
    }

    const auto indexData = PFUInt16VectorFromTriangles(triangles);

    auto *vb = VertexBuffer::Builder()
        .vertexCount(positions.size())
        .bufferCount(1)
        .attribute(VertexAttribute::POSITION, 0, VertexBuffer::AttributeType::FLOAT3, offsetof(MorphRenderableVertex, px), sizeof(MorphRenderableVertex))
        .attribute(VertexAttribute::TANGENTS, 0, VertexBuffer::AttributeType::SHORT4, offsetof(MorphRenderableVertex, tx), sizeof(MorphRenderableVertex))
        .normalized(VertexAttribute::TANGENTS)
        .attribute(VertexAttribute::UV0, 0, VertexBuffer::AttributeType::FLOAT2, offsetof(MorphRenderableVertex, u), sizeof(MorphRenderableVertex))
        .build(*_engine);
    const size_t vertexDataSize = vertexData.size() * sizeof(MorphRenderableVertex);
    vb->setBufferAt(*_engine, 0, VertexBuffer::BufferDescriptor(
        PFMakeOwnedCopy(vertexData.data(), vertexDataSize),
        vertexDataSize,
        [](void *buffer, size_t, void *) { free(buffer); }
    ));
    _vertexBuffers.push_back(vb);

    auto *ib = IndexBuffer::Builder()
        .indexCount((uint32_t)indexData.size())
        .bufferType(IndexBuffer::IndexType::USHORT)
        .build(*_engine);
    const size_t indexDataSize = indexData.size() * sizeof(uint16_t);
    ib->setBuffer(*_engine, IndexBuffer::BufferDescriptor(
        PFMakeOwnedCopy(indexData.data(), indexDataSize),
        indexDataSize,
        [](void *buffer, size_t, void *) { free(buffer); }
    ));
    _indexBuffers.push_back(ib);

    auto* morphTargetBuffer = MorphTargetBuffer::Builder()
        .vertexCount(positions.size())
        .count((size_t)morphTargetCount)
        .withPositions(true)
        .withTangents(true)
        .build(*_engine);

    for (size_t targetIndex = 0; targetIndex < morphTargets.size(); targetIndex++) {
        auto const& targetPositions = morphTargets[targetIndex];
        morphTargetBuffer->setPositionsAt(*_engine, targetIndex, targetPositions.data(), targetPositions.size());
        auto* targetOrientation = filament::geometry::SurfaceOrientation::Builder()
            .vertexCount(positions.size())
            .positions(targetPositions.data())
            .uvs(uvs.data())
            .triangleCount(triangles.size())
            .triangles(triangles.data())
            .build();
        std::vector<short4> targetTangents(positions.size());
        targetOrientation->getQuats(targetTangents.data(), positions.size());
        delete targetOrientation;
        morphTargetBuffer->setTangentsAt(*_engine, targetIndex, targetTangents.data(), targetTangents.size());
    }

    const auto bounds = PFBoundsFromMorphData(positions, morphTargets);

    Entity entity = EntityManager::get().create();
    RenderableManager::Builder(1)
        .geometry(0, RenderableManager::PrimitiveType::TRIANGLES, vb, ib)
        .material(0, it->second)
        .morphing(morphTargetBuffer)
        .boundingBox(bounds)
        .culling(false)
        .build(*_engine, entity);

    _scene->addEntity(entity);
    int handle = _nextHandle++;
    _renderables[handle] = entity;
    _morphTargetBuffers[handle] = morphTargetBuffer;
    return handle;
}

- (void)setRenderableRotation:(int)handle rotationX:(float)rotationX rotationY:(float)rotationY {
    auto it = _renderables.find(handle);
    if (it == _renderables.end() || !_engine) return;

    auto& transformManager = _engine->getTransformManager();
    auto instance = transformManager.getInstance(it->second);
    if (!instance.isValid()) return;

    const float radiansX = rotationX * (float)M_PI / 180.0f;
    const float radiansY = rotationY * (float)M_PI / 180.0f;
    const mat4f rotation = mat4f::rotation(radiansX, float3{1.0f, 0.0f, 0.0f}) *
        mat4f::rotation(radiansY, float3{0.0f, 1.0f, 0.0f});
    transformManager.setTransform(instance, rotation);
}

- (void)setRenderableTransform:(int)handle
                           m00:(float)m00 m01:(float)m01 m02:(float)m02 m03:(float)m03
                           m10:(float)m10 m11:(float)m11 m12:(float)m12 m13:(float)m13
                           m20:(float)m20 m21:(float)m21 m22:(float)m22 m23:(float)m23
                           m30:(float)m30 m31:(float)m31 m32:(float)m32 m33:(float)m33 {
    auto it = _renderables.find(handle);
    if (it == _renderables.end() || !_engine) return;

    auto& transformManager = _engine->getTransformManager();
    auto instance = transformManager.getInstance(it->second);
    if (!instance.isValid()) return;

    transformManager.setTransform(instance, mat4f{
        float4{m00, m01, m02, m03},
        float4{m10, m11, m12, m13},
        float4{m20, m21, m22, m23},
        float4{m30, m31, m32, m33},
    });
}

- (void)setMorphWeights:(int)handle weights:(NSArray<NSNumber *> *)weights {
    auto it = _renderables.find(handle);
    if (it == _renderables.end() || !_engine || weights.count == 0) return;

    auto& renderableManager = _engine->getRenderableManager();
    auto instance = renderableManager.getInstance(it->second);
    if (!instance.isValid()) return;

    std::vector<float> values;
    values.reserve(weights.count);
    for (NSNumber *value in weights) {
        values.push_back(value.floatValue);
    }
    renderableManager.setMorphWeights(instance, values.data(), values.size());
}

- (void)removeRenderable:(int)handle {
    auto it = _renderables.find(handle);
    if (it == _renderables.end() || !_engine) return;
    auto morphIt = _morphTargetBuffers.find(handle);
    _scene->remove(it->second);
    if (morphIt != _morphTargetBuffers.end()) {
        _engine->destroy(morphIt->second);
        _morphTargetBuffers.erase(morphIt);
    }
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
}

@end
