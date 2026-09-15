import os
import re

src_dir = '/Users/nerandadilhara/Desktop/SLIIT/4Y2S/Enterprise Application Development - SE4040 /Assignment/SmartSolarMicrogrid/web/smart-solar-web/src'

for root, _, files in os.walk(src_dir):
    for file in files:
        if file.endswith('.tsx') or file.endswith('.ts'):
            path = os.path.join(root, file)
            with open(path, 'r') as f:
                content = f.read()

            # Clean up trailing garbage after Promise.resolve
            content = re.sub(r'Promise\.resolve\(\{data: null, error: null\}\),\s*[a-zA-Z0-9_\.]+\);', 'Promise.resolve({data: null, error: null});', content)

            with open(path, 'w') as f:
                f.write(content)
