"""Extract the simulator from the Cobblemon JAR resolved and verified by Gradle."""
import io
import shutil
import sys
import zipfile
from pathlib import Path
from build_support import ROOT, extract_safe


def main():
    source, destination = map(Path, sys.argv[1:])
    expected = (ROOT / 'build/showdown').resolve()
    if destination.resolve() != expected:
        raise ValueError('Simulator output must be the generated build/showdown directory')
    with zipfile.ZipFile(source) as archive:
        with zipfile.ZipFile(io.BytesIO(archive.read('data/cobblemon/showdown.zip'))) as simulator:
            if expected.exists():
                shutil.rmtree(expected)
            extract_safe(simulator, expected)


if __name__ == '__main__':
    main()
