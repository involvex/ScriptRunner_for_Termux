package io.github.swiftstagrime.termuxrunner.data.template

import io.github.swiftstagrime.termuxrunner.domain.model.ScriptTemplate

val BUILTIN_TEMPLATES =
    listOf(
        ScriptTemplate(
            id = "bash_hello",
            name = "Hello World",
            description = "A simple bash script that prints a greeting",
            content = "#!/bin/bash\necho 'Hello, World!'\n",
            category = "bash",
        ),
        ScriptTemplate(
            id = "bash_backup",
            name = "Simple Backup Script",
            description = "Compresses a directory into a timestamped backup",
            content = "#!/bin/bash\nSOURCE_DIR=${'$'}{1:-${'$'}HOME}\nBACKUP_DIR=${'$'}HOME/backups\nTIMESTAMP=${'$'}(date +\"%Y%m%d_%H%M%S\")\nmkdir -p \"${'$'}BACKUP_DIR\"\ntar -czf \"${'$'}BACKUP_DIR/backup_${'$'}TIMESTAMP.tar.gz\" \"${'$'}SOURCE_DIR\"\necho \"Backup created: ${'$'}BACKUP_DIR/backup_${'$'}TIMESTAMP.tar.gz\"\n",
            category = "bash",
        ),
        ScriptTemplate(
            id = "bash_network_check",
            name = "Network Availability Check",
            description = "Checks if specific URLs are reachable",
            content = "#!/bin/bash\nURLS=('https://google.com' 'https://github.com' 'https://example.com')\nfor url in \"${'$'}{URLS[@]}\"; do\n    if curl -s --connect-timeout 5 \"${'$'}url\" > /dev/null; then\n        echo \"OK: ${'$'}url\"\n    else\n        echo \"FAIL: ${'$'}url\"\n    fi\ndone\n",
            category = "bash",
        ),
        ScriptTemplate(
            id = "python_data_processing",
            name = "Data Processing",
            description = "Python script template for data processing tasks",
            content = "#!/usr/bin/env python3\nimport sys\nfrom pathlib import Path\n\ndef process_file(filepath: str) -> dict:\n    '''Process a file and return results.'''\n    path = Path(filepath)\n    if not path.exists():\n        print(f'Error: {filepath} not found', file=sys.stderr)\n        return {'status': 'error', 'message': f'{filepath} not found'}\n\n    content = path.read_text()\n    lines = len(content.splitlines())\n    words = len(content.split())\n\n    return {\n        'status': 'success',\n        'filename': path.name,\n        'lines': lines,\n        'words': words,\n    }\n\nif __name__ == '__main__':\n    if len(sys.argv) < 2:\n        print('Usage: script.py <file>')\n        sys.exit(1)\n\n    filepath = sys.argv[1]\n    result = process_file(filepath)\n    print(result)\n",
            category = "python",
        ),
        ScriptTemplate(
            id = "python_api_client",
            name = "API Client Template",
            description = "Python template for making HTTP API requests",
            content = "#!/usr/bin/env python3\nimport requests\nimport sys\nimport json\n\nAPI_BASE = 'https://api.example.com/v1'\n\ndef make_request(endpoint: str, method: str = \"GET\", data: dict = None) -> dict:\n    '''Make an API request and return the response.'''\n    url = f'{API_BASE}/{endpoint}'\n    headers = {'Content-Type': 'application/json'}\n    try:\n        response = requests.request(\n            method=method,\n            url=url,\n            headers=headers,\n            json=data,\n            timeout=30,\n        )\n        response.raise_for_status()\n        return response.json()\n    except requests.exceptions.RequestException as e:\n        print(f'Request error: {e}', file=sys.stderr)\n        return {'error': str(e)}\n\nif __name__ == '__main__':\n    result = make_request('status')\n    print(json.dumps(result, indent=2))\n",
            category = "python",
        ),
        ScriptTemplate(
            id = "node_web_server",
            name = "Simple HTTP Server",
            description = "A minimal Node.js HTTP server template",
            content = "const http = require('http');\n\nconst hostname = '127.0.0.1';\nconst port = process.env.PORT || 3000;\n\nconst server = http.createServer((req, res) => {\n    console.log(`Request: ${'$'}{req.method} ${'$'}{req.url}`);\n\n    if (req.url === '/') {\n        res.statusCode = 200;\n        res.setHeader('Content-Type', 'application/json');\n        res.end(JSON.stringify({ status: 'ok', timestamp: new Date() }));\n    } else {\n        res.statusCode = 404;\n        res.setHeader('Content-Type', 'text/plain');\n        res.end('Not Found');\n    }\n});\n\nserver.listen(port, hostname, () => {\n    console.log(`Server running at http://${'$'}{hostname}:${'$'}{port}/`);\n});\n",
            category = "node",
        ),
        ScriptTemplate(
            id = "powershell_system_info",
            name = "System Info Report",
            description = "Gathers system information on Windows",
            content = "Get-ComputerInfo | Select-Object WindowsProductName, WindowsVersion, TotalPhysicalMemory, CsProcessors\n\nWrite-Host '--- Disk Usage ---'\nGet-PSDrive -PSProvider FileSystem | Format-Table Name, Used, Free\n\nWrite-Host '--- Network Adapters ---'\nGet-NetIPAddress -AddressFamily IPv4 | Where-Object {${'$'}_.InterfaceAlias -notmatch 'Loopback'} | Format-Table InterfaceAlias, IPAddress, PrefixLength\n",
            category = "powershell",
        ),
    )
