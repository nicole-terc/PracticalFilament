#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
FILAMENT_DIR="${FILAMENT_DIR:-$ROOT_DIR/tools/filament/1.70.1}"
MATERIALS_SOURCE_DIR="${MATERIALS_SOURCE_DIR:-$ROOT_DIR/filament-assets/materials}"
ENVIRONMENTS_SOURCE_DIR="${ENVIRONMENTS_SOURCE_DIR:-$ROOT_DIR/filament-assets/envs}"
MODELS_SOURCE_DIR="${MODELS_SOURCE_DIR:-$ROOT_DIR/filament-assets/models}"
RESOURCES_DIR="${RESOURCES_DIR:-$ROOT_DIR/composeApp/src/commonMain/composeResources/files}"
MATERIALS_OUTPUT_DIR="${MATERIALS_OUTPUT_DIR:-$RESOURCES_DIR/materials}"
ENVIRONMENT_OUTPUT_DIR="${ENVIRONMENT_OUTPUT_DIR:-$RESOURCES_DIR/envs}"
MODELS_OUTPUT_DIR="${MODELS_OUTPUT_DIR:-$RESOURCES_DIR/models}"
CMGEN_SIZE="${CMGEN_SIZE:-256}"
CMGEN_EXTRACT_BLUR="${CMGEN_EXTRACT_BLUR:-0.1}"

MATC="$FILAMENT_DIR/bin/matc"
CMGEN="$FILAMENT_DIR/bin/cmgen"
FILAMESH="$FILAMENT_DIR/bin/filamesh"

usage() {
    cat <<EOF
Usage: $(basename "$0") [all|materials|environments]

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
        "$FILAMESH" --compress "$source_file" "$output_base">/dev/null
    done < <(find "$MODELS_SOURCE_DIR" -type f -name '*.obj' | LC_ALL=C sort)

    if [[ "$found" -eq 0 ]]; then
        echo "No .obj files found in $MODELS_SOURCE_DIR"
    fi
}

main() {
    local mode="${1:-all}"
    mkdir -p "$RESOURCES_DIR" "$MATERIALS_OUTPUT_DIR" "$ENVIRONMENT_OUTPUT_DIR"

    case "$mode" in
        all)
            require_tool "$MATC"
            require_tool "$CMGEN"
            require_tool "$FILAMESH"
            build_materials
            build_environments
            build_models
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
