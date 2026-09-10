package io.github.swiftstagrime.termuxrunner.data.template

import io.github.swiftstagrime.termuxrunner.domain.model.ScriptTemplate

val BUILTIN_TEMPLATES =
    listOf(
        ScriptTemplate(
            id = "bash_hello",
            name = "Hello World",
            description = "A simple bash script that prints a greeting",
            content = """
                #!/bin/bash
                echo 'Hello, World!'
            """.trimIndent(),
            category = "bash",
        ),
        ScriptTemplate(
            id = "bash_backup",
            name = "Simple Backup Script",
            description = "Compresses a directory into a timestamped backup",
            content = """
                #!/bin/bash
                SOURCE_DIR=${'$'}{1:-${'$'}HOME}
                BACKUP_DIR=${'$'}HOME/backups
                TIMESTAMP=${'$'}(date +"%Y%m%d_%H%M%S")
                mkdir -p "${'$'}BACKUP_DIR"
                tar -czf "${'$'}BACKUP_DIR/backup_${'$'}TIMESTAMP.tar.gz" "${'$'}SOURCE_DIR"
                echo "Backup created: ${'$'}BACKUP_DIR/backup_${'$'}TIMESTAMP.tar.gz"
            """.trimIndent(),
            category = "bash",
        ),
        ScriptTemplate(
            id = "bash_network_check",
            name = "Network Availability Check",
            description = "Checks if specific URLs are reachable",
            content = """
                #!/bin/bash
                URLS=('https://google.com' 'https://github.com' 'https://example.com')
                for url in "${'$'}{URLS[@]}"; do
                    if curl -s --connect-timeout 5 "${'$'}url" > /dev/null; then
                        echo "OK: ${'$'}url"
                    else
                        echo "FAIL: ${'$'}url"
                    fi
                done
            """.trimIndent(),
            category = "bash",
        ),
        ScriptTemplate(
            id = "python_data_processing",
            name = "Data Processing",
            description = "Python script template for data processing tasks",
            content = """
                #!/usr/bin/env python3
                import sys
                from pathlib import Path

                def process_file(filepath: str) -> dict:
                    '''Process a file and return results.'''
                    path = Path(filepath)
                    if not path.exists():
                        print(f'Error: {filepath} not found', file=sys.stderr)
                        return {'status': 'error', 'message': f'{filepath} not found'}

                    content = path.read_text()
                    lines = len(content.splitlines())
                    words = len(content.split())

                    return {
                        'status': 'success',
                        'filename': path.name,
                        'lines': lines,
                        'words': words,
                    }

                if __name__ == '__main__':
                    if len(sys.argv) < 2:
                        print('Usage: script.py <file>')
                        sys.exit(1)

                    filepath = sys.argv[1]
                    result = process_file(filepath)
                    print(result)
            """.trimIndent(),
            category = "python",
        ),
        ScriptTemplate(
            id = "python_api_client",
            name = "API Client Template",
            description = "Python template for making HTTP API requests",
            content = """
                #!/usr/bin/env python3
                import requests
                import sys
                import json

                API_BASE = 'https://api.example.com/v1'

                def make_request(endpoint: str, method: str = "GET", data: dict = None) -> dict:
                    '''Make an API request and return the response.'''
                    url = f'{API_BASE}/{endpoint}'
                    headers = {'Content-Type': 'application/json'}
                    try:
                        response = requests.request(
                            method=method,
                            url=url,
                            headers=headers,
                            json=data,
                            timeout=30,
                        )
                        response.raise_for_status()
                        return response.json()
                    except requests.exceptions.RequestException as e:
                        print(f'Request error: {e}', file=sys.stderr)
                        return {'error': str(e)}

                if __name__ == '__main__':
                    result = make_request('status')
                    print(json.dumps(result, indent=2))
            """.trimIndent(),
            category = "python",
        ),
        ScriptTemplate(
            id = "node_web_server",
            name = "Simple HTTP Server",
            description = "A minimal Node.js HTTP server template",
            content = """
                const http = require('http');

                const hostname = '127.0.0.1';
                const port = process.env.PORT || 3000;

                const server = http.createServer((req, res) => {
                    console.log(`Request: ${'$'}{req.method} ${'$'}{req.url}`);

                    if (req.url === '/') {
                        res.statusCode = 200;
                        res.setHeader('Content-Type', 'application/json');
                        res.end(JSON.stringify({ status: 'ok', timestamp: new Date() }));
                    } else {
                        res.statusCode = 404;
                        res.setHeader('Content-Type', 'text/plain');
                        res.end('Not Found');
                    }
                });

                server.listen(port, hostname, () => {
                    console.log(`Server running at http://${'$'}{hostname}:${'$'}{port}/`);
                });
            """.trimIndent(),
            category = "node",
        ),
        ScriptTemplate(
            id = "powershell_system_info",
            name = "System Info Report",
            description = "Gathers system information on Windows",
            content = """
                Get-ComputerInfo | Select-Object WindowsProductName, WindowsVersion, TotalPhysicalMemory, CsProcessors

                Write-Host '--- Disk Usage ---'
                Get-PSDrive -PSProvider FileSystem | Format-Table Name, Used, Free

                Write-Host '--- Network Adapters ---'
                Get-NetIPAddress -AddressFamily IPv4 | Where-Object {${'$'}_.InterfaceAlias -notmatch 'Loopback'} | Format-Table InterfaceAlias, IPAddress, PrefixLength
            """.trimIndent(),
            category = "powershell",
        ),
    )
