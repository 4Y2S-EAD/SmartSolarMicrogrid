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
            # Match Promise.resolve({data: [], error: null}) followed by space and random stuff until comma or end of line
            content = re.sub(r'Promise\.resolve\(\{data: \[\], error: null\}\)[^,;\n]*', 'Promise.resolve({data: [], error: null})', content)
            
            # Match Promise.resolve({data: null, error: null}) followed by random stuff
            content = re.sub(r'Promise\.resolve\(\{data: null, error: null\}\)[^,;\n]*', 'Promise.resolve({data: null, error: null})', content)

            with open(path, 'w') as f:
                f.write(content)
