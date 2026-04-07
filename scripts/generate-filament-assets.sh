#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
FILAMENT_DIR="${FILAMENT_DIR:-$ROOT_DIR/tools/filament/1.70.1}"
MATERIALS_SOURCE_DIR="${MATERIALS_SOURCE_DIR:-$ROOT_DIR/filament-assets/materials}"
ENVIRONMENTS_SOURCE_DIR="${ENVIRONMENTS_SOURCE_DIR:-$ROOT_DIR/filament-assets/envs}"
MODELS_SOURCE_DIR="${MODELS_SOURCE_DIR:-$ROOT_DIR/filament-assets/models}"
TEXTURES_SOURCE_DIR="${TEXTURES_SOURCE_DIR:-$ROOT_DIR/filament-assets/textures}"
RESOURCES_DIR="${RESOURCES_DIR:-$ROOT_DIR/composeApp/src/commonMain/composeResources/files}"
MATERIALS_OUTPUT_DIR="${MATERIALS_OUTPUT_DIR:-$RESOURCES_DIR/materials}"
ENVIRONMENT_OUTPUT_DIR="${ENVIRONMENT_OUTPUT_DIR:-$RESOURCES_DIR/envs}"
MODELS_OUTPUT_DIR="${MODELS_OUTPUT_DIR:-$RESOURCES_DIR/models}"
TEXTURES_OUTPUT_DIR="${TEXTURES_OUTPUT_DIR:-$RESOURCES_DIR/textures}"
CMGEN_SIZE="${CMGEN_SIZE:-256}"
CMGEN_EXTRACT_BLUR="${CMGEN_EXTRACT_BLUR:-0.1}"

MATC="$FILAMENT_DIR/bin/matc"
CMGEN="$FILAMENT_DIR/bin/cmgen"
FILAMESH="$FILAMENT_DIR/bin/filamesh"
MIPGEN="$FILAMENT_DIR/bin/mipgen"

usage() {
    cat <<EOF
Usage: $(basename "$0") [all|materials|environments|textures]

Compiles Filament source assets into Compose resources.

Defaults:
  Filament SDK: $FILAMENT_DIR
  Materials:    $MATERIALS_SOURCE_DIR
  Environments: $ENVIRONMENTS_SOURCE_DIR
  Materials out:$MATERIALS_OUTPUT_DIR
  Envs out:     $ENVIRONMENT_OUTPUT_DIR
EOF
}

require_tool() {
    local tool_path="$1"
    if [[ ! -x "$tool_path" ]]; then
        echo "Missing executable: $tool_path" >&2
        exit 1
    fi
}

# Texture type classification by filename pattern (case-insensitive).
# albedo/color textures are sRGB; all others (normal, roughness, ao, arm, disp) are linear.
_texture_is_linear() {
    local name
    name="$(basename "$1" | tr '[:upper:]' '[:lower:]')"
    # Match both prefixed names (e.g. moss_normal.png) and bare names (e.g. normal.png).
    case "$name" in
        normal*|*_nor_*|*[_-]normal*|\
        roughness*|*[_-]rough*|\
        ao*|*[_-]ao*|\
        metallic*|*[_-]metallic*|\
        *[_-]arm*|*[_-]disp*|*[_-]height*) return 0 ;;
        *) return 1 ;;
    esac
}

# Normal maps need orientation-aware downsampling.
_texture_is_normal() {
    local name
    name="$(basename "$1" | tr '[:upper:]' '[:lower:]')"
    case "$name" in
        normal*|*_nor_*|*[_-]normal*) return 0 ;;
        *) return 1 ;;
    esac
}

build_textures() {
    local found=0

    if [[ ! -d "$TEXTURES_SOURCE_DIR" ]]; then
        echo "Skipping textures: source directory not found at $TEXTURES_SOURCE_DIR"
        return
    fi

    local tmp_dir
    tmp_dir="$(mktemp -d)"

    while IFS= read -r source_file; do
        local relative_path output_file output_dir mipgen_flags mipgen_input
        found=1
        relative_path="${source_file#"$TEXTURES_SOURCE_DIR"/}"
        # Strip extension and replace with .ktx
        output_file="$TEXTURES_OUTPUT_DIR/${relative_path%.*}.ktx"
        output_dir="$(dirname "$output_file")"

        mkdir -p "$output_dir"

        mipgen_flags="--format=ktx"
        if _texture_is_linear "$source_file"; then
            mipgen_flags="$mipgen_flags --linear"
        fi
        if _texture_is_normal "$source_file"; then
            mipgen_flags="$mipgen_flags --kernel=normals"
        fi

        # mipgen only reads PNG; convert JPEGs with sips first.
        mipgen_input="$source_file"
        case "${source_file##*.}" in
            jpg|jpeg|JPG|JPEG)
                local tmp_png
                tmp_png="$tmp_dir/$(basename "${source_file%.*}").png"
                sips -s format png "$source_file" --out "$tmp_png" >/dev/null 2>&1
                mipgen_input="$tmp_png"
                ;;
        esac

        echo "mipgen $relative_path -> ${output_file#"$ROOT_DIR"/}"
        # shellcheck disable=SC2086
        "$MIPGEN" $mipgen_flags -q "$mipgen_input" "$output_file"
    done < <(find "$TEXTURES_SOURCE_DIR" -type f \( -iname '*.jpg' -o -iname '*.jpeg' -o -iname '*.png' \) | LC_ALL=C sort)

    rm -rf "$tmp_dir"

    if [[ "$found" -eq 0 ]]; then
        echo "No texture files found in $TEXTURES_SOURCE_DIR"
    fi
}

build_materials() {
    local found=0

    if [[ ! -d "$MATERIALS_SOURCE_DIR" ]]; then
        echo "Skipping materials: source directory not found at $MATERIALS_SOURCE_DIR"
        return
    fi

    while IFS= read -r source_file; do
        local relative_path output_file output_dir
        found=1
        relative_path="${source_file#"$MATERIALS_SOURCE_DIR"/}"
        output_file="$MATERIALS_OUTPUT_DIR/${relative_path%.mat}.filamat"
        output_dir="$(dirname "$output_file")"

        mkdir -p "$output_dir"
        echo "matc  $relative_path -> ${output_file#"$ROOT_DIR"/}"
        "$MATC" -p mobile -a all -o "$output_file" "$source_file"
    done < <(find "$MATERIALS_SOURCE_DIR" -type f -name '*.mat' | LC_ALL=C sort)

    if [[ "$found" -eq 0 ]]; then
        echo "No .mat files found in $MATERIALS_SOURCE_DIR"
    fi
}

build_environments() {
    local found=0

    if [[ ! -d "$ENVIRONMENTS_SOURCE_DIR" ]]; then
        echo "Skipping environments: source directory not found at $ENVIRONMENTS_SOURCE_DIR"
        return
    fi

    while IFS= read -r source_file; do
        local relative_path output_base output_parent
        found=1
        relative_path="${source_file#"$ENVIRONMENTS_SOURCE_DIR"/}"
        output_base="$ENVIRONMENT_OUTPUT_DIR/${relative_path%.hdr}"
        output_parent="$(dirname "$output_base")"

        mkdir -p "$output_parent"
        rm -rf "$output_base"
        echo "cmgen $relative_path -> ${output_base#"$ROOT_DIR"/}/"
        "$CMGEN" -x "$output_base" --format=ktx --size="$CMGEN_SIZE" --extract-blur="$CMGEN_EXTRACT_BLUR" "$source_file" >/dev/null
    done < <(find "$ENVIRONMENTS_SOURCE_DIR" -type f -name '*.hdr' | LC_ALL=C sort)

    if [[ "$found" -eq 0 ]]; then
        echo "No .hdr files found in $ENVIRONMENTS_SOURCE_DIR"
    fi
}

build_models(){
    local found=0

    if [[ ! -d "$MODELS_SOURCE_DIR" ]]; then
        echo "Skipping models: source directory not found at $MODELS_SOURCE_DIR"
        return
    fi

    while IFS= read -r source_file; do
        local relative_path output_base output_parent
        found=1
        relative_path="${source_file#"$MODELS_SOURCE_DIR"/}"
        output_base="$MODELS_OUTPUT_DIR/${relative_path%.obj}.filamesh"
        output_parent="$(dirname "$output_base")"

        mkdir -p "$output_parent"
        rm -rf "$output_base"
        echo "filamesh $relative_path -> ${output_base#"$ROOT_DIR"/}/"
        "$FILAMESH" "$source_file" "$output_base">/dev/null
    done < <(find "$MODELS_SOURCE_DIR" -type f -name '*.obj' | LC_ALL=C sort)

    if [[ "$found" -eq 0 ]]; then
        echo "No .obj files found in $MODELS_SOURCE_DIR"
    fi
}

main() {
    local mode="${1:-all}"
    mkdir -p "$RESOURCES_DIR" "$MATERIALS_OUTPUT_DIR" "$ENVIRONMENT_OUTPUT_DIR" "$TEXTURES_OUTPUT_DIR"

    case "$mode" in
        all)
            require_tool "$MATC"
            require_tool "$CMGEN"
            require_tool "$FILAMESH"
            require_tool "$MIPGEN"
            build_materials
            build_environments
            build_models
            build_textures
            ;;
        materials)
            require_tool "$MATC"
            build_materials
            ;;
        environments|envs)
            require_tool "$CMGEN"
            build_environments
            ;;
        models)
            require_tool "$FILAMESH"
            build_models
            ;;
        textures)
            require_tool "$MIPGEN"
            build_textures
            ;;
        -h|--help|help)
            usage
            ;;
        *)
            usage >&2
            exit 1
            ;;
    esac
}

main "$@"
