# Practical Filament
Codebase for Practical Filament talk :D

Sample code using Google's [Filament](https://github.com/google/filament) framework

## Interesting Links
- Framework repository: [google/filament](https://github.com/google/filament/tree/main)
- Trove of assets: [polyhaven.com](https://polyhaven.com/)

## Asset generation

Filament's macOS SDK is bundled in `tools/filament/1.70.1` so material and environment assets can be rebuilt locally without depending on a separate desktop install.

- Put source materials in `filament-assets/materials/*.mat`
- Put source HDR environments in `filament-assets/envs/*.hdr`
- Run `./scripts/generate-filament-assets.sh`

The script compiles `.mat` files into `composeApp/src/commonMain/composeResources/files/materials/*.filamat` and generates environment KTX outputs under `composeApp/src/commonMain/composeResources/files/envs/`.

### Asset disclaimer

This demo includes several assets from [google/filament](https://github.com/google/filament/tree/main) and [polyhaven.com](https://polyhaven.com/). They belong to their original authors and keep their original licenses.


## Gotchas

- Ensure that the material filamat files are built with the same version of Filament as the one used in the app.
- Filament directional light vectors point in the direction the light emits, not toward the light source. With the camera placed on `+Z` and looking at the origin, a front light needs a negative `z` direction. Using positive `z` back-lights the sphere and can make lit materials look almost black.
- For lit custom geometry, `VertexAttribute.TANGENTS` must contain Filament tangent-space quaternions, not plain normals. On Android, use `SurfaceOrientation` to generate these instead of hand-rolling tangent data.
