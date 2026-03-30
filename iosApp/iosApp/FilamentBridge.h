#import <Foundation/Foundation.h>
#import <QuartzCore/QuartzCore.h>

@interface FilamentBridge : NSObject

- (void)initializeWithMetalLayer:(CAMetalLayer * _Nonnull)layer
                           width:(int)width
                          height:(int)height;
- (void)setClearColorR:(float)r g:(float)g b:(float)b a:(float)a;
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
            falloffRadius:(float)falloffRadius
                   dirX:(float)dirX dirY:(float)dirY dirZ:(float)dirZ
              innerCone:(float)innerCone outerCone:(float)outerCone
      sunAngularRadius:(float)sunAngularRadius
            sunHaloSize:(float)sunHaloSize
         sunHaloFalloff:(float)sunHaloFalloff;

- (void)removeLight:(int)handle;
- (void)clearLights;

- (int)loadIndirectLightFromPath:(NSString * _Nonnull)path;
- (void)setIndirectLight:(int)handle intensity:(float)intensity;
- (int)loadSkyboxFromPath:(NSString * _Nonnull)path;
- (void)setSkybox:(int)handle;

- (int)loadMaterialFromPath:(NSString * _Nonnull)path;
- (int)getMaterialParameterDefinitionCount:(int)materialHandle;
- (NSString * _Nonnull)getMaterialParameterName:(int)materialHandle index:(int)index;
- (NSString * _Nonnull)getMaterialParameterTypeName:(int)materialHandle index:(int)index;
- (NSString * _Nonnull)getMaterialParameterPrecisionName:(int)materialHandle index:(int)index;
- (int)getMaterialParameterArraySize:(int)materialHandle index:(int)index;
- (int)createMaterialInstance:(int)materialHandle;

- (void)setBoolParam:(int)instanceHandle name:(NSString * _Nonnull)name value:(BOOL)value;
- (void)setFloatParam:(int)instanceHandle name:(NSString * _Nonnull)name value:(float)value;
- (void)setIntParam:(int)instanceHandle name:(NSString * _Nonnull)name value:(int)value;
- (void)setBool2Param:(int)instanceHandle name:(NSString * _Nonnull)name
                    x:(BOOL)x y:(BOOL)y;
- (void)setBool3Param:(int)instanceHandle name:(NSString * _Nonnull)name
                    x:(BOOL)x y:(BOOL)y z:(BOOL)z;
- (void)setBool4Param:(int)instanceHandle name:(NSString * _Nonnull)name
                    x:(BOOL)x y:(BOOL)y z:(BOOL)z w:(BOOL)w;
- (void)setFloat2Param:(int)instanceHandle name:(NSString * _Nonnull)name
                     x:(float)x y:(float)y;
- (void)setFloat3Param:(int)instanceHandle name:(NSString * _Nonnull)name
                     x:(float)x y:(float)y z:(float)z;
- (void)setFloat4Param:(int)instanceHandle name:(NSString * _Nonnull)name
                     x:(float)x y:(float)y z:(float)z w:(float)w;
- (void)setInt2Param:(int)instanceHandle name:(NSString * _Nonnull)name
                   x:(int)x y:(int)y;
- (void)setInt3Param:(int)instanceHandle name:(NSString * _Nonnull)name
                   x:(int)x y:(int)y z:(int)z;
- (void)setInt4Param:(int)instanceHandle name:(NSString * _Nonnull)name
                   x:(int)x y:(int)y z:(int)z w:(int)w;
- (void)setMat3Param:(int)instanceHandle name:(NSString * _Nonnull)name
                   m00:(float)m00 m01:(float)m01 m02:(float)m02
                   m10:(float)m10 m11:(float)m11 m12:(float)m12
                   m20:(float)m20 m21:(float)m21 m22:(float)m22;
- (void)setMat4Param:(int)instanceHandle name:(NSString * _Nonnull)name
                   m00:(float)m00 m01:(float)m01 m02:(float)m02 m03:(float)m03
                   m10:(float)m10 m11:(float)m11 m12:(float)m12 m13:(float)m13
                   m20:(float)m20 m21:(float)m21 m22:(float)m22 m23:(float)m23
                   m30:(float)m30 m31:(float)m31 m32:(float)m32 m33:(float)m33;

- (int)createTextureWithWidth:(int)width height:(int)height pixels:(NSData * _Nonnull)pixels;
- (void)setTextureParam:(int)instanceHandle name:(NSString * _Nonnull)name textureHandle:(int)textureHandle;

- (int)createPlaneWithMaterial:(int)instanceHandle width:(float)width height:(float)height;
- (int)createSphereWithMaterial:(int)instanceHandle radius:(float)radius;
- (int)createMorphRenderableWithMaterial:(int)instanceHandle
                               positions:(NSData * _Nonnull)positions
                                      uvs:(NSData * _Nonnull)uvs
                                  indices:(NSData * _Nonnull)indices
                      morphTargetPositions:(NSData * _Nonnull)morphTargetPositions
                          morphTargetCount:(int)morphTargetCount;
- (void)setRenderableRotation:(int)handle rotationX:(float)rotationX rotationY:(float)rotationY;
- (void)setRenderableTransform:(int)handle
                           m00:(float)m00 m01:(float)m01 m02:(float)m02 m03:(float)m03
                           m10:(float)m10 m11:(float)m11 m12:(float)m12 m13:(float)m13
                           m20:(float)m20 m21:(float)m21 m22:(float)m22 m23:(float)m23
                           m30:(float)m30 m31:(float)m31 m32:(float)m32 m33:(float)m33;
- (void)setMorphWeights:(int)handle weights:(NSArray<NSNumber *> * _Nonnull)weights;
- (void)removeRenderable:(int)handle;

- (void)render;
- (void)updateViewportWidth:(int)width height:(int)height;

@end
