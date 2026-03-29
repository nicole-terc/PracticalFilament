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
#import <utils/EntityManager.h>
#import <math/vec3.h>
#import <math/mat4.h>

#include <cmath>
#include <map>
#include <vector>

using namespace filament;
using namespace utils;
using namespace math;

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

    for (auto *vb : _vertexBuffers) _engine->destroy(vb);
    _vertexBuffers.clear();
    for (auto *ib : _indexBuffers) _engine->destroy(ib);
    _indexBuffers.clear();

    _engine->destroyView(_view);
    _engine->destroyScene(_scene);
    _engine->destroyCameraComponent(_cameraEntity);
    _engine->destroyRenderer(_renderer);
    if (_swapChain) _engine->destroySwapChain(_swapChain);

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
    NSData *data = [NSData dataWithContentsOfFile:path];
    if (!data) return -1;

    Material *mat = Material::Builder()
        .package(data.bytes, data.length)
        .build(*_engine);

    int handle = _nextHandle++;
    _materials[handle] = mat;
    return handle;
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

- (void)setIntParam:(int)instanceHandle name:(NSString *)name value:(int)value {
    auto it = _materialInstances.find(instanceHandle);
    if (it == _materialInstances.end()) return;
    it->second->setParameter(name.UTF8String, value);
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

- (int)createPlaneWithMaterial:(int)instanceHandle width:(float)width height:(float)height {
    if (!_engine) return -1;
    auto it = _materialInstances.find(instanceHandle);
    if (it == _materialInstances.end()) return -1;

    float hw = width / 2.0f;
    float hh = height / 2.0f;

    // 4 vertices: position(3) + normal(3) + uv(2) = 8 floats each
    float vertices[] = {
        -hw, -hh, 0, 0, 0, 1, 0, 0,
         hw, -hh, 0, 0, 0, 1, 1, 0,
         hw,  hh, 0, 0, 0, 1, 1, 1,
        -hw,  hh, 0, 0, 0, 1, 0, 1,
    };
    uint16_t indices[] = {0, 1, 2, 0, 2, 3};

    auto *vb = VertexBuffer::Builder()
        .vertexCount(4)
        .bufferCount(1)
        .attribute(VertexAttribute::POSITION, 0, VertexBuffer::AttributeType::FLOAT3, 0, 32)
        .attribute(VertexAttribute::TANGENTS, 0, VertexBuffer::AttributeType::FLOAT3, 12, 32)
        .attribute(VertexAttribute::UV0, 0, VertexBuffer::AttributeType::FLOAT2, 24, 32)
        .build(*_engine);
    vb->setBufferAt(*_engine, 0, VertexBuffer::BufferDescriptor(vertices, sizeof(vertices)));
    _vertexBuffers.push_back(vb);

    auto *ib = IndexBuffer::Builder()
        .indexCount(6)
        .bufferType(IndexBuffer::IndexType::USHORT)
        .build(*_engine);
    ib->setBuffer(*_engine, IndexBuffer::BufferDescriptor(indices, sizeof(indices)));
    _indexBuffers.push_back(ib);

    Entity entity = EntityManager::get().create();
    RenderableManager::Builder(1)
        .geometry(0, RenderableManager::PrimitiveType::TRIANGLES, vb, ib)
        .material(0, it->second)
        .boundingBox({{-hw, -hh, -0.01f}, {hw, hh, 0.01f}})
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
    vb->setBufferAt(*_engine, 0,
        VertexBuffer::BufferDescriptor(vertexData.data(), vertexData.size() * sizeof(float)));
    _vertexBuffers.push_back(vb);

    auto *ib = IndexBuffer::Builder()
        .indexCount(indexCount)
        .bufferType(IndexBuffer::IndexType::USHORT)
        .build(*_engine);
    ib->setBuffer(*_engine,
        IndexBuffer::BufferDescriptor(indexData.data(), indexData.size() * sizeof(uint16_t)));
    _indexBuffers.push_back(ib);

    Entity entity = EntityManager::get().create();
    RenderableManager::Builder(1)
        .geometry(0, RenderableManager::PrimitiveType::TRIANGLES, vb, ib)
        .material(0, it->second)
        .boundingBox({{-radius, -radius, -radius}, {radius, radius, radius}})
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
