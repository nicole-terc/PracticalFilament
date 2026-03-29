# Practical Filament
Codebase for Practical Filament talk :D

Sample code using Google's [Filament](https://github.com/google/filament) framework

## Gotchas

- Filament directional light vectors point in the direction the light emits, not toward the light source. With the camera placed on `+Z` and looking at the origin, a front light needs a negative `z` direction. Using positive `z` back-lights the sphere and can make lit materials look almost black.
- For lit custom geometry, `VertexAttribute.TANGENTS` must contain Filament tangent-space quaternions, not plain normals. On Android, use `SurfaceOrientation` to generate these instead of hand-rolling tangent data.
