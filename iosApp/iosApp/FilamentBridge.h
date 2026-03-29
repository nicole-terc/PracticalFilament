@import Foundation;
@import QuartzCore;

@interface FilamentBridge : NSObject

- (void)initializeWithMetalLayer:(CAMetalLayer * _Nonnull)layer
                           width:(int)width
                          height:(int)height;
- (void)destroy;
- (void)clearScene;

- (void)updateCameraEyeX:(float)eyeX eyeY:(float)eyeY eyeZ:(float)eyeZ
                  targetX:(float)targetX targetY:(float)targetY targetZ:(float)targetZ
                      upX:(float)upX upY:(float)upY upZ:(float)upZ
                      fov:(double)fov near:(double)near far:(double)far;

- (int)addLightWithType:(int)type
                      r:(float)r g:(float)g b:(float)b
              intensity:(float)intensity
                   posX:(float)posX posY:(float)posY posZ:(float)posZ
                   dirX:(float)dirX dirY:(float)dirY dirZ:(float)dirZ
              innerCone:(float)innerCone outerCone:(float)outerCone;

- (void)removeLight:(int)handle;
- (void)clearLights;

- (int)loadMaterialFromPath:(NSString * _Nonnull)path;
- (int)createMaterialInstance:(int)materialHandle;

- (void)setFloatParam:(int)instanceHandle name:(NSString * _Nonnull)name value:(float)value;
- (void)setIntParam:(int)instanceHandle name:(NSString * _Nonnull)name value:(int)value;
- (void)setFloat3Param:(int)instanceHandle name:(NSString * _Nonnull)name
                     x:(float)x y:(float)y z:(float)z;
- (void)setFloat4Param:(int)instanceHandle name:(NSString * _Nonnull)name
                     x:(float)x y:(float)y z:(float)z w:(float)w;

- (int)createPlaneWithMaterial:(int)instanceHandle width:(float)width height:(float)height;
- (int)createSphereWithMaterial:(int)instanceHandle radius:(float)radius;
- (void)removeRenderable:(int)handle;

- (void)render;
- (void)updateViewportWidth:(int)width height:(int)height;

@end
