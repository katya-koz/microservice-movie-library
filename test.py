from pathlib import Path

# Directories to search
SOURCE_DIRS = [
    Path(r"C:\Users\Katyousha\Documents\Personal Projects\KatyaFlix2\katyaflix\frontend\katyaflix\hooks"),
    Path(r"C:\Users\Katyousha\Documents\Personal Projects\KatyaFlix2\katyaflix\frontend\katyaflix\api"),
    Path(r"C:\Users\Katyousha\Documents\Personal Projects\KatyaFlix2\katyaflix\frontend\katyaflix\lib"),
    Path(r"C:\Users\Katyousha\Documents\Personal Projects\KatyaFlix2\katyaflix\frontend\katyaflix\app"),
    Path(r"C:\Users\Katyousha\Documents\Personal Projects\KatyaFlix2\katyaflix\frontend\katyaflix\types"),
    Path(r"C:\Users\Katyousha\Documents\Personal Projects\KatyaFlix2\katyaflix\frontend\katyaflix\components"),
]

# All generated files go here
OUTPUT_DIR = Path("./combined_output")

CODE_EXTENSIONS = {
    ".ts", ".tsx",
    ".js", ".jsx",
    ".java",
    ".py",
    ".cs",
    ".cpp", ".c", ".h", ".hpp",
    ".html",
    ".css", ".scss",
    ".sql",
    ".json",
    ".yaml", ".yml",
    ".xml",
    ".sh", ".bat",
}

IGNORED_DIRS = {
    "node_modules",
    ".git",
    ".next",
    "dist",
    "build",
    "target",
    "out",
}


def combine_directory(source_dir):
    # Use the directory name for the output filename
    output_file = OUTPUT_DIR / f"{source_dir.name}_combined.txt"

    with output_file.open("w", encoding="utf-8") as output:

        for file_path in sorted(source_dir.rglob("*")):

            if not file_path.is_file():
                continue

            if any(part in IGNORED_DIRS for part in file_path.parts):
                continue

            if file_path.suffix.lower() not in CODE_EXTENSIONS:
                continue

            relative_path = file_path.relative_to(source_dir)

            print(f"[{source_dir.name}] Adding: {relative_path}")

            output.write("\n")
            output.write("=" * 80 + "\n")
            output.write(f"FILE: {relative_path}\n")
            output.write("=" * 80 + "\n\n")

            try:
                output.write(file_path.read_text(encoding="utf-8"))
            except UnicodeDecodeError:
                print(f"Skipping non-UTF-8 file: {file_path}")
                continue

            output.write("\n\n")

    print(f"Created: {output_file}")


def main():
    # Create output directory if it doesn't exist
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)

    for source_dir in SOURCE_DIRS:

        if not source_dir.exists():
            print(f"Directory does not exist: {source_dir}")
            continue

        combine_directory(source_dir)


if __name__ == "__main__":
    main()