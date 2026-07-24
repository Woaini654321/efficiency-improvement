import sys

with open(sys.argv[1], 'r', encoding='utf-8') as f:
    lines = f.readlines()

in_script = False
depth = 0
last_depth_0_line = 0

for i, line in enumerate(lines, 1):
    if '<script' in line and 'babel' in line:
        in_script = True
        continue
    if in_script and '</script>' in line:
        in_script = False
        continue
    if not in_script:
        continue

    for ch in line:
        if ch == '{':
            depth += 1
        elif ch == '}':
            depth -= 1
            if depth < 0:
                printable = line.strip()[:200]
                print(f'Line {i}: NEGATIVE depth={depth}')
                print(f'  {printable}')

    if depth == 0:
        last_depth_0_line = i

print(f'\nFinal depth: {depth}')
print(f'Last line where depth was 0: {last_depth_0_line}')
