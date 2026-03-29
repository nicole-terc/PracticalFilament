import Foundation
import ComposeApp
import QuartzCore

private final class DisplayLinkProxy {
    var onFrame: (() -> Void)?

    @objc
    func step() {
        onFrame?()
    }
}

/// Swift adapter that bridges the ObjC++ FilamentBridge to the Kotlin FilamentBridgeProtocol.
class FilamentBridgeAdapter: FilamentBridgeProtocol {
    private let bridge = FilamentBridge()
    private let displayLinkProxy = DisplayLinkProxy()
    private var displayLink: CADisplayLink?

    init() {
        displayLinkProxy.onFrame = { [weak self] in
            self?.bridge.render()
        }
    }

    func initializeWithMetalLayer(layer: CAMetalLayer, width: Int32, height: Int32) {
        bridge.initialize(with: layer, width: width, height: height)
        startRenderLoop()
    }

    func setClearColorRGBA(r: Float, g: Float, b: Float, a: Float) {
        bridge.setClearColorR(r, g: g, b: b, a: a)
    }

    func destroy() {
        stopRenderLoop()
        bridge.destroy()
    }
    func clearScene() { bridge.clearScene() }

    func updateCameraEyeX(eyeX: Float, eyeY: Float, eyeZ: Float,
                           targetX: Float, targetY: Float, targetZ: Float,
                           upX: Float, upY: Float, upZ: Float,
                           fov: Double, near: Double, far: Double) {
        bridge.updateCameraEyeX(eyeX, eyeY: eyeY, eyeZ: eyeZ,
                                targetX: targetX, targetY: targetY, targetZ: targetZ,
                                upX: upX, upY: upY, upZ: upZ,
                                fov: fov, near: near, far: far)
    }

    func addLightWithType(type: Int32, r: Float, g: Float, b: Float,
                          intensity: Float,
                          posX: Float, posY: Float, posZ: Float,
                          falloffRadius: Float,
                          dirX: Float, dirY: Float, dirZ: Float,
                          innerCone: Float, outerCone: Float,
                          sunAngularRadius: Float,
                          sunHaloSize: Float,
                          sunHaloFalloff: Float) -> Int32 {
        return bridge.addLight(withType: type,
                               r: r, g: g, b: b,
                               intensity: intensity,
                               posX: posX, posY: posY, posZ: posZ,
                               falloffRadius: falloffRadius,
                               dirX: dirX, dirY: dirY, dirZ: dirZ,
                               innerCone: innerCone, outerCone: outerCone,
                               sunAngularRadius: sunAngularRadius,
                               sunHaloSize: sunHaloSize,
                               sunHaloFalloff: sunHaloFalloff)
    }

    func removeLight(handle: Int32) { bridge.removeLight(handle) }
    func clearLights() { bridge.clearLights() }

    func loadIndirectLightFromPath(path: String) -> Int32 {
        bridge.loadIndirectLight(fromPath: path)
    }

    func setIndirectLight(handle: Int32, intensity: Float) {
        bridge.setIndirectLight(handle, intensity: intensity)
    }

    func loadSkyboxFromPath(path: String) -> Int32 {
        bridge.loadSkybox(fromPath: path)
    }

    func setSkybox(handle: Int32) {
        bridge.setSkybox(handle)
    }

    func loadMaterialFromPath(path: String) -> Int32 {
        return bridge.loadMaterial(fromPath: path)
    }

    func getMaterialParameterDefinitionCount(materialHandle: Int32) -> Int32 {
        return bridge.getMaterialParameterDefinitionCount(materialHandle)
    }

    func getMaterialParameterName(materialHandle: Int32, index: Int32) -> String {
        return bridge.getMaterialParameterName(materialHandle, index: index)
    }

    func getMaterialParameterTypeName(materialHandle: Int32, index: Int32) -> String {
        return bridge.getMaterialParameterTypeName(materialHandle, index: index)
    }

    func getMaterialParameterPrecisionName(materialHandle: Int32, index: Int32) -> String {
        return bridge.getMaterialParameterPrecisionName(materialHandle, index: index)
    }

    func getMaterialParameterArraySize(materialHandle: Int32, index: Int32) -> Int32 {
        return bridge.getMaterialParameterArraySize(materialHandle, index: index)
    }

    func createMaterialInstance(materialHandle: Int32) -> Int32 {
        return bridge.createMaterialInstance(materialHandle)
    }

    func setBoolParam(instanceHandle: Int32, name: String, value: Bool) {
        bridge.setBoolParam(instanceHandle, name: name, value: value)
    }

    func setFloatParam(instanceHandle: Int32, name: String, value: Float) {
        bridge.setFloatParam(instanceHandle, name: name, value: value)
    }

    func setIntParam(instanceHandle: Int32, name: String, value: Int32) {
        bridge.setIntParam(instanceHandle, name: name, value: value)
    }

    func setBool2Param(instanceHandle: Int32, name: String, x: Bool, y: Bool) {
        bridge.setBool2Param(instanceHandle, name: name, x: x, y: y)
    }

    func setBool3Param(instanceHandle: Int32, name: String, x: Bool, y: Bool, z: Bool) {
        bridge.setBool3Param(instanceHandle, name: name, x: x, y: y, z: z)
    }

    func setBool4Param(instanceHandle: Int32, name: String, x: Bool, y: Bool, z: Bool, w: Bool) {
        bridge.setBool4Param(instanceHandle, name: name, x: x, y: y, z: z, w: w)
    }

    func setFloat2Param(instanceHandle: Int32, name: String, x: Float, y: Float) {
        bridge.setFloat2Param(instanceHandle, name: name, x: x, y: y)
    }

    func setFloat3Param(instanceHandle: Int32, name: String, x: Float, y: Float, z: Float) {
        bridge.setFloat3Param(instanceHandle, name: name, x: x, y: y, z: z)
    }

    func setFloat4Param(instanceHandle: Int32, name: String, x: Float, y: Float, z: Float, w: Float) {
        bridge.setFloat4Param(instanceHandle, name: name, x: x, y: y, z: z, w: w)
    }

    func setInt2Param(instanceHandle: Int32, name: String, x: Int32, y: Int32) {
        bridge.setInt2Param(instanceHandle, name: name, x: x, y: y)
    }

    func setInt3Param(instanceHandle: Int32, name: String, x: Int32, y: Int32, z: Int32) {
        bridge.setInt3Param(instanceHandle, name: name, x: x, y: y, z: z)
    }

    func setInt4Param(instanceHandle: Int32, name: String, x: Int32, y: Int32, z: Int32, w: Int32) {
        bridge.setInt4Param(instanceHandle, name: name, x: x, y: y, z: z, w: w)
    }

    func setMat3Param(instanceHandle: Int32, name: String,
                      m00: Float, m01: Float, m02: Float,
                      m10: Float, m11: Float, m12: Float,
                      m20: Float, m21: Float, m22: Float) {
        bridge.setMat3Param(instanceHandle, name: name,
                            m00: m00, m01: m01, m02: m02,
                            m10: m10, m11: m11, m12: m12,
                            m20: m20, m21: m21, m22: m22)
    }

    func setMat4Param(instanceHandle: Int32, name: String,
                      m00: Float, m01: Float, m02: Float, m03: Float,
                      m10: Float, m11: Float, m12: Float, m13: Float,
                      m20: Float, m21: Float, m22: Float, m23: Float,
                      m30: Float, m31: Float, m32: Float, m33: Float) {
        bridge.setMat4Param(instanceHandle, name: name,
                            m00: m00, m01: m01, m02: m02, m03: m03,
                            m10: m10, m11: m11, m12: m12, m13: m13,
                            m20: m20, m21: m21, m22: m22, m23: m23,
                            m30: m30, m31: m31, m32: m32, m33: m33)
    }

    func createTextureWithWidth(width: Int32, height: Int32, pixels: KotlinByteArray) -> Int32 {
        let count = Int(pixels.size)
        var data = Data(count: count)
        data.withUnsafeMutableBytes { rawBuffer in
            guard let destination = rawBuffer.bindMemory(to: UInt8.self).baseAddress else { return }
            for index in 0..<count {
                destination[index] = UInt8(bitPattern: pixels.get(index: Int32(index)))
            }
        }
        return bridge.createTexture(withWidth: width, height: height, pixels: data)
    }

    func setTextureParam(instanceHandle: Int32, name: String, textureHandle: Int32) {
        bridge.setTextureParam(instanceHandle, name: name, textureHandle: textureHandle)
    }

    func createPlaneWithMaterial(instanceHandle: Int32, width: Float, height: Float) -> Int32 {
        return bridge.createPlane(withMaterial: instanceHandle, width: width, height: height)
    }

    func createSphereWithMaterial(instanceHandle: Int32, radius: Float) -> Int32 {
        return bridge.createSphere(withMaterial: instanceHandle, radius: radius)
    }

    func setRenderableRotation(handle: Int32, rotationX: Float, rotationY: Float) {
        bridge.setRenderableRotation(handle, rotationX: rotationX, rotationY: rotationY)
    }

    func setRenderableTransform(handle: Int32,
                                m00: Float, m01: Float, m02: Float, m03: Float,
                                m10: Float, m11: Float, m12: Float, m13: Float,
                                m20: Float, m21: Float, m22: Float, m23: Float,
                                m30: Float, m31: Float, m32: Float, m33: Float) {
        bridge.setRenderableTransform(handle,
                                      m00: m00, m01: m01, m02: m02, m03: m03,
                                      m10: m10, m11: m11, m12: m12, m13: m13,
                                      m20: m20, m21: m21, m22: m22, m23: m23,
                                      m30: m30, m31: m31, m32: m32, m33: m33)
    }

    func removeRenderable(handle: Int32) { bridge.removeRenderable(handle) }
    func render() { bridge.render() }

    func updateViewportWidth(width: Int32, height: Int32) {
        bridge.updateViewportWidth(width, height: height)
    }

    private func startRenderLoop() {
        guard displayLink == nil else { return }
        let displayLink = CADisplayLink(target: displayLinkProxy, selector: #selector(DisplayLinkProxy.step))
        displayLink.add(to: .main, forMode: .common)
        self.displayLink = displayLink
    }

    private func stopRenderLoop() {
        displayLink?.invalidate()
        displayLink = nil
    }
}
