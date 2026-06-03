import json

with open('/Users/daniel.notthoff/.gemini/antigravity/brain/2833713c-e9fb-4dd2-8072-99f262446583/.system_generated/logs/transcript.jsonl', 'r') as f:
    for line in f:
        try:
            data = json.loads(line)
            if data.get('type') == 'VIEW_FILE' and 'todo.md' in data.get('content', ''):
                if 'FamWake – TODO' in data.get('content', ''):
                    print(data['content'])
                    break
        except Exception as e:
            pass
