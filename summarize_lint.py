import re
from collections import Counter
import sys

def summarize_lint(filepath):
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()
    except Exception as e:
        print(f"Error reading {filepath}: {e}")
        return

    # Look for lines like: ... Warning: ... [WarningId]
    pattern = re.compile(r'Warning:.*?\[([a-zA-Z0-9_]+)\]')
    warnings = pattern.findall(content)
    
    counts = Counter(warnings)
    print("Lint Warning Summary:")
    for warn_id, count in counts.most_common():
        print(f"{count}x: {warn_id}")
        
    print("\nTotal warnings:", sum(counts.values()))

if __name__ == '__main__':
    summarize_lint(sys.argv[1])
