**Language**: [English](README.md) | [中文](README_cn.md)

# Codex Plugin for Jenkins

A Jenkins plugin that provides AI-powered analysis capabilities for pipeline stages, steps, and script output using the [Codex CLI](https://github.com/openai/codex).

## Features

- **Pipeline Step Analysis**: Use the `codexAnalysis` step in your Pipeline scripts
- **Interactive Chat**: Use the `codexChat` step for real-time interactive chat sessions with Codex CLI
- **Build Dropdown Chat**: Access Codex Chat directly from the build dropdown menu (next to "Timings") for every job
- **Stage-level Analysis**: Automatic analysis of pipeline stages with context gathering
- **Freestyle Job Support**: Add Codex analysis or interactive chat as build steps in freestyle jobs
- **Multiple Analysis Types**: Build, test, deployment, security, performance, and quality analysis
- **Real-Time Console Logging**: All chat conversations are streamed to Jenkins console output in real-time
- **CLI-Only Model Management**: Dynamic model lists fetched directly from Codex CLI (no hardcoded models)
- **Real-Time Model Updates**: Model lists always reflect current CLI capabilities
- **MCP Servers Support**: Model Context Protocol servers for enhanced analysis capabilities
- **Dynamic MCP Management**: Update MCP servers list directly from Codex CLI with caching
- **Job-Level Configuration**: Configure Codex settings per job with node-specific testing
- **Rich UI**: Detailed analysis results with issue detection and summaries
- **CLI Management**: Download and update Codex CLI directly from Jenkins (job-level)

## Prerequisites

Before using this plugin, ensure you have:

1. **Codex CLI installed and configured**
   - Follow the installation guide at [codex.sh](https://github.com/codex-router/codex.sh/blob/main/README.md)
   - Ensure the CLI is in your system PATH
   - Configure your API keys and model settings
   - **Important**: Model lists are fetched dynamically from Codex CLI - no hardcoded models are provided
   - **Note**: The Codex CLI Download URL configuration is optional - you can install the CLI manually or use the plugin's download feature

2. **Network access** to the model provider's API

## Installation

1. Download the plugin HPI file from the releases page
2. Go to **Manage Jenkins** → **Manage Plugins** → **Advanced**
3. Upload the HPI file in the "Upload Plugin" section
4. Restart Jenkins

## Configuration

### Global Configuration

Configure the plugin globally in **Manage Jenkins** → **Configure System** → **Codex Analysis Plugin**:

![System Configuration](global.png)

- **Codex CLI Path**: Path to the Codex CLI executable (default: "~/.local/bin/codex")
- **Codex CLI Download URL**: Download URL for Codex CLI on Ubuntu/CentOS systems (optional)
- **Codex CLI Download Username**: Username for authenticated download URL (optional)
- **Codex CLI Download Password**: Password for authenticated download URL (optional)
- **Config Path**: Path to Codex configuration file (default: "~/.codex/config.toml")
- **Timeout**: Default timeout for analysis operations in seconds (default: 120)
- **LiteLLM API Key**: API key for LiteLLM service (default: empty)

**Note**: Default Model and MCP Servers configuration are only available at the job level for more granular control.

### Job-Level Configuration

You can also configure Codex settings per job by adding the **Codex Analysis Plugin Configuration** in the job's configuration page:

![Job Configuration](job.png)

1. Go to your job's configuration page
2. Scroll down to find **Codex Analysis Plugin Configuration**
3. Enable **Use Job-Level Configuration** to override global settings
4. Configure job-specific settings:
   - **Codex CLI Path**: Override the global CLI path for this job
   - **Codex CLI Download URL**: Override the global CLI download URL for this job (optional)
   - **Codex CLI Download Username**: Override the global CLI download username for this job
   - **Codex CLI Download Password**: Override the global CLI download password for this job
   - **Manual CLI Update**: Use the "Update CLI" button to manually download and update Codex CLI from the download URL (job-level only)
   - **Config Path**: Override the global config path for this job
   - **Default Model**: Configure the default model for this job (dropdown is empty by default - must click "Update Model List" to populate)
     - Use the "Update Model List" button to fetch available models from Codex CLI
     - Model list is populated dynamically from Codex CLI - no hardcoded models
     - Dropdown starts empty and only shows models after fetching from CLI
   - **Timeout**: Override the global timeout for this job
   - **Enable MCP Servers**: Enable Model Context Protocol servers for this job (default: disabled)
   - **MCP Servers**: Select MCP servers for this job (only shown when 'Enable MCP Servers' is checked)
     - Dropdown is empty by default - must click "Update MCP Servers List" to populate
     - Use the "Update MCP Servers List" button to fetch available servers from Codex CLI
     - MCP servers configuration is job-specific
   - **LiteLLM API Key**: Override the global LiteLLM API key for this job (default: empty)
5. Use the **Test Codex CLI** button to verify the CLI is accessible on the node where this job will run
6. Use the **Update CLI** button to manually download and update the Codex CLI when needed

**Note**: CLI testing and updating are only available at the job level to ensure proper node binding. This allows you to test and update the Codex CLI configuration in the context of the specific node where your job will execute.

### MCP Servers Configuration

The plugin supports Model Context Protocol (MCP) servers for enhanced analysis capabilities. MCP servers provide additional tools and context to the Codex CLI during analysis. The plugin reads MCP server configurations from `~/.codex/config.toml` and allows you to select which servers to enable for analysis.

**Prerequisites:**
- MCP servers must be configured in `~/.codex/config.toml` following the [Codex CLI documentation](https://github.com/openai/codex/blob/main/docs/config.md#mcp-cli-commands)
- The plugin will automatically detect available MCP servers from the configuration file
- All required MCP server executables and dependencies must be installed on the system

**How It Works:**
1. **Configuration Detection**: The plugin reads `~/.codex/config.toml` and extracts MCP server names
2. **Server Selection**: Users can select which servers to enable from a dropdown list
3. **Multi-Selection**: Multiple servers can be selected for combined functionality
4. **Empty by Default**: The dropdown starts empty - use "Update MCP Servers List" to populate it
5. **Dynamic Updates**: Use the "Update MCP Servers List" button to refresh the server list from Codex CLI

#### MCP Server Types

**1. stdio Servers**
- **Purpose**: Local command-line tools and scripts
- **Configuration**: Command and arguments to execute
- **Use Cases**: File system tools, local utilities, custom scripts

**2. http Servers**
- **Purpose**: Remote HTTP-based services
- **Configuration**: URL endpoint and authentication
- **Use Cases**: Web APIs, remote services, cloud-based tools

#### MCP Server Configuration Fields

Each MCP server can be configured with the following fields:

- **Name**: Unique identifier for the server
- **Type**: Server type (stdio or http)
- **Enabled**: Whether this server is active
- **Command**: Command to execute (stdio servers only)
- **Arguments**: Command line arguments (stdio servers only)
- **URL**: HTTP endpoint URL (http servers only)
- **Bearer Token Environment Variable**: Environment variable containing authentication token (http servers only)
- **Startup Timeout**: Timeout for server startup (default: 10 seconds)
- **Tool Timeout**: Timeout for individual tool calls (default: 60 seconds)

#### Configuration Examples

**Example ~/.codex/config.toml with MCP Servers:**
```toml
[mcp.servers."filesystem"]
type = "stdio"
command = "/usr/local/bin/mcp-filesystem"
args = ["--root", "/workspace"]
startup_timeout_sec = 10
tool_timeout_sec = 60

[mcp.servers."github-api"]
type = "http"
url = "https://api.github.com/mcp"
bearer_token_env_var = "GITHUB_TOKEN"
startup_timeout_sec = 15
tool_timeout_sec = 120

[mcp.servers."database"]
type = "stdio"
command = "/opt/mcp-tools/db-connector"
args = ["--host", "localhost", "--port", "5432", "--database", "myapp"]
startup_timeout_sec = 20
tool_timeout_sec = 90
```

**Jenkins Plugin Selection:**
- In the Jenkins configuration, you would see a dropdown with options: "filesystem", "github-api", "database"
- You can select multiple servers (e.g., "filesystem" and "github-api") for multi-selection

#### MCP Server Configuration Workflow

**Step 1: Configure MCP Servers in ~/.codex/config.toml**
1. Edit the `~/.codex/config.toml` file to define your MCP servers
2. Follow the [Codex CLI documentation](https://github.com/openai/codex/blob/main/docs/config.md#mcp-cli-commands) for proper configuration format
3. Ensure all required commands and dependencies are installed

**Step 2: Enable MCP Servers in Jenkins**
1. Check the "Enable MCP Servers" checkbox in global or job configuration
2. The "MCP Servers" selection list will become visible

**Step 3: Select MCP Servers**
1. Choose which MCP servers to enable from the dropdown list
2. The list shows all servers configured in `~/.codex/config.toml`
3. You can select multiple servers for multi-selection

**Step 4: Test and Validate**
1. Use the "Test Codex CLI" button to verify configuration
2. Check Jenkins logs for any MCP server startup issues
3. Monitor analysis performance with MCP servers enabled

### Configuration Hierarchy

The plugin uses a three-tier configuration hierarchy:

1. **Step/Builder Level** (Highest Priority)
   - Parameters specified directly in the `codexAnalysis` or `codexChat` step or build step
   - Overrides both job-level and global settings

2. **Job Level** (Medium Priority)
   - Settings configured in the job's "Codex Analysis Plugin Configuration"
   - Only applies when "Use Job-Level Configuration" is enabled
   - Overrides global settings

3. **Global Level** (Lowest Priority)
   - System-wide settings in "Manage Jenkins" → "Configure System"
   - Used as fallback when job-level settings are empty or disabled

**Note**: Both `codexAnalysis` and `codexChat` steps respect this configuration hierarchy for model selection, timeout, and other settings.

### Best Practices

- **Global Configuration**: Set sensible defaults for your organization
- **Job-Level Configuration**: Use for jobs that need specific CLI paths or models
- **Node Testing**: Always test CLI accessibility at the job level to ensure proper node binding
- **Fallback Strategy**: Leave job-level settings empty to inherit global defaults

## Usage

### Pipeline Step Usage

Use the `codexAnalysis` step in your Pipeline scripts:

```groovy
pipeline {
    agent any

    stages {
        stage('Build') {
            steps {
                sh 'mvn clean compile'
                codexAnalysis(
                    content: 'Build completed successfully',
                    analysisType: 'build_analysis',
                    prompt: 'Analyze this build output and suggest improvements'
                )
            }
        }

        stage('Test') {
            steps {
                sh 'mvn test'
                codexAnalysis(
                    content: 'Test execution completed',
                    analysisType: 'test_analysis',
                    includeContext: true
                )
            }
        }
    }
}
```

### Interactive Chat Usage

Use the `codexChat` step for interactive chat sessions with Codex CLI. All conversations are logged to the console in real-time:

```groovy
pipeline {
    agent any

    stages {
        stage('Interactive Chat') {
            steps {
                codexChat(
                    initialMessage: 'Hello, can you help me analyze my build?',
                    model: 'gpt-4',
                    timeoutSeconds: 300
                )
            }
        }
    }
}
```

**Chat Step Parameters:**
- **initialMessage**: Optional initial message to start the chat session
- **context**: Optional context information (if not provided, build context is automatically included)
- **model**: Model to use for chat (optional, uses default from job or global config)
- **timeoutSeconds**: Timeout for chat session in seconds (default: 120)
- **additionalParams**: Additional parameters in key=value format

### Freestyle Job Usage

#### Codex Analysis Build Step

1. Go to your job configuration
2. Add "Codex Analysis" build step
3. Configure the analysis parameters:
   - **Content to Analyze**: Text content to analyze
   - **Analysis Type**: Type of analysis to perform
   - **Custom Prompt**: Optional custom prompt
   - **Model**: Model to use (optional, uses default if empty)
   - **Timeout**: Analysis timeout in seconds
   - **Include Build Context**: Include build environment in analysis
   - **Fail on Error**: Fail build if analysis encounters errors
   - **Additional Parameters**: Custom parameters in key=value format

#### Codex Interactive Chat Build Step

1. Go to your job configuration
2. Add "Codex Interactive Chat" build step
3. Configure the chat parameters:
   - **Initial Message**: Optional initial message to start the chat session
   - **Context**: Optional context information (if not provided, build context is automatically included)
   - **Model**: Model to use for chat (optional, uses default from job or global config)
   - **Timeout**: Chat session timeout in seconds (default: 120)
   - **Additional Parameters**: Custom parameters in key=value format

**Note**: All chat conversations are streamed to the Jenkins console output in real-time, making them visible for all Jenkins jobs.

### Build Dropdown Chat

The plugin automatically adds a **"Codex Chat"** option to the build dropdown menu (next to "Timings") for every job. This allows you to interact with Codex CLI directly from any build without modifying your pipeline or job configuration.

![Codex Chat in Build Dropdown](chat-1.png)

**How to use:**

1. Navigate to any build in Jenkins
2. Click on the build dropdown menu (the small arrow next to the build number)
3. Select **"Codex Chat"** from the menu
4. You'll see a chat interface where you can:

![Codex Chat Interface](chat-2.png)
   - Enter an initial message to start the conversation
   - Provide optional context about the build (logs, environment, etc.)
   - Send messages and receive responses from Codex CLI
   - View the conversation history

**Features:**
- **Automatic Availability**: Available for every build automatically - no configuration needed
- **Uses Configured CLI Path**: Automatically uses the Codex CLI path from global or job configuration
- **Build Context**: Chat executes in the build's workspace context with access to build environment
- **Interactive Interface**: Web-based chat interface with message history
- **Real-time Responses**: Get immediate responses from Codex CLI

**Configuration:**
- The chat feature uses the Codex CLI path configured in:
  - Job-level configuration (if "Use Job-Level Configuration" is enabled)
  - Global configuration (as fallback)
- All chat interactions respect the configured model, timeout, and MCP server settings

**Example Use Cases:**
- Ask questions about build failures
- Get explanations of build logs
- Request suggestions for improving build performance
- Analyze build artifacts or test results
- Get help with deployment issues

### Analysis Types

- **general**: General analysis and insights
- **build_analysis**: Build process and output analysis
- **test_analysis**: Test results and coverage analysis
- **deployment_analysis**: Deployment process analysis
- **security_analysis**: Security vulnerability analysis
- **performance_analysis**: Performance metrics analysis
- **quality_analysis**: Code quality and best practices analysis

## Examples

### Comprehensive CI/CD Pipeline

```groovy
pipeline {
    agent any

    stages {
        stage('Build') {
            steps {
                sh 'mvn clean compile'
                codexAnalysis(
                    content: 'Maven compilation completed',
                    analysisType: 'build_analysis',
                    includeContext: true
                )
            }
        }

        stage('Test') {
            steps {
                sh 'mvn test'
                codexAnalysis(
                    content: 'Unit tests executed',
                    analysisType: 'test_analysis',
                    prompt: 'Review test execution and suggest improvements'
                )
            }
        }

        stage('Security Scan') {
            steps {
                sh 'mvn dependency:check'
                codexAnalysis(
                    content: 'Security dependency check completed',
                    analysisType: 'security_analysis',
                    includeContext: true
                )
            }
        }

        stage('Deploy') {
            when {
                branch 'main'
            }
            steps {
                sh 'mvn deploy'
                codexAnalysis(
                    content: 'Application deployed successfully',
                    analysisType: 'deployment_analysis',
                    includeContext: true
                )
            }
        }
    }

    post {
        always {
            codexAnalysis(
                content: 'Pipeline execution completed',
                analysisType: 'general',
                prompt: 'Provide a comprehensive analysis of this CI/CD pipeline execution'
            )
        }
    }
}
```

### Advanced Analysis with Custom Parameters

```groovy
pipeline {
    agent any

    stages {
        stage('Advanced Analysis') {
            steps {
                script {
                    def buildOutput = sh(
                        script: 'mvn clean compile 2>&1',
                        returnStdout: true
                    ).trim()

                    def analysisResult = codexAnalysis(
                        content: buildOutput,
                        analysisType: 'build_analysis',
                        model: 'gpt-4',
                        timeoutSeconds: 180,
                        additionalParams: [
                            'temperature': '0.7',
                            'max_tokens': '2000',
                            'include_code': 'true'
                        ]
                    )

                    echo "Analysis result: ${analysisResult}"
                }
            }
        }
    }
}
```

### Interactive Chat with Build Context

```groovy
pipeline {
    agent any

    stages {
        stage('Build') {
            steps {
                sh 'mvn clean compile'
            }
        }

        stage('Chat Session') {
            steps {
                // Start interactive chat with build context automatically included
                codexChat(
                    initialMessage: 'Can you help me understand any issues in my build?',
                    model: 'gpt-4',
                    timeoutSeconds: 300
                )
            }
        }
    }
}
```

### Chat with Custom Context

```groovy
pipeline {
    agent any

    stages {
        stage('Chat with Custom Context') {
            steps {
                script {
                    def customContext = """
                        Project: My Application
                        Build Number: ${env.BUILD_NUMBER}
                        Branch: ${env.BRANCH_NAME}
                        Recent Changes: ${sh(script: 'git log -5 --oneline', returnStdout: true)}
                    """.trim()

                    codexChat(
                        initialMessage: 'Review my recent changes and suggest improvements',
                        context: customContext,
                        model: 'claude-3-opus',
                        timeoutSeconds: 600
                    )
                }
            }
        }
    }
}
```

### Job-Level Configuration Example

For jobs that need specific Codex CLI configurations:

1. **Configure Job-Level Settings**:
   - Go to your job's configuration page
   - Find "Codex Analysis Plugin Configuration"
   - Enable "Use Job-Level Configuration"
   - Set job-specific values:
     - **Codex CLI Path**: `/usr/local/bin/codex` (if different from global)
     - **Default Model**: `claude-3-opus` (for this specific job)
     - **Timeout**: `300` (longer timeout for complex analysis)

2. **Test Configuration**:
   - Click "Test Codex CLI" to verify the CLI is accessible on the node
   - Ensure the test passes before running the job

3. **Use in Pipeline**:
   ```groovy
   pipeline {
       agent any

       stages {
           stage('Analysis') {
               steps {
                   // This will use job-level configuration
                   codexAnalysis(
                       content: 'Build output here',
                       analysisType: 'build_analysis'
                       // model and timeout will be inherited from job config
                   )
               }
           }
       }
   }
   ```

## Troubleshooting

### Common Issues

1. **Codex CLI not found**
   - Ensure the CLI is installed and in PATH
   - Check the "Codex CLI Path" configuration (global or job-level)
   - Use the job-level "Test Codex CLI" button to verify node accessibility
   - **Note**: If you need to download the CLI automatically, configure the "Codex CLI Download URL" (optional)

2. **Timeout errors**
   - Increase the timeout value for complex analyses
   - Check network connectivity to API endpoints
   - Verify timeout settings in job-level configuration

3. **Job-level configuration not working**
   - Ensure "Use Job-Level Configuration" is enabled in job settings
   - Check that job-level values are not empty (empty values fall back to global)
   - Verify the job-level test passes before running the job

4. **Node-specific issues**
   - Test CLI accessibility on the specific node where the job runs
   - Ensure Codex CLI is installed on all nodes, not just the master
   - Check node-specific PATH and environment variables

5. **MCP Servers not available**
   - **Expected Behavior**: The dropdown is empty by default - this is normal. You must click "Update MCP Servers List" to populate it.
   - Ensure MCP servers are properly configured in `~/.codex/config.toml`
   - Use the "Update MCP Servers List" button to refresh the server list from Codex CLI
   - Check that the config file path is correct and accessible
   - Verify MCP server executables are installed and in PATH
   - If CLI fails, the plugin will fall back to parsing the config file directly

6. **Model list not updating**
   - Use the "Update Model List" button to refresh available models from Codex CLI
   - Check that the Codex CLI is properly configured with API keys
   - Verify network connectivity to model provider APIs
   - Model list is cached for 5 minutes - wait for cache expiration or restart Jenkins

7. **No models available in dropdown**
   - **Expected Behavior**: The dropdown is empty by default - this is normal. You must click "Update Model List" to populate it.
   - **CLI-Only Approach**: The plugin fetches models exclusively from Codex CLI - no hardcoded models are provided
   - Ensure Codex CLI is properly installed and accessible
   - Use the "Update Model List" button to fetch models from CLI
   - Check that `codex models list` command works in your terminal
   - Verify API keys are configured in Codex CLI
   - If no models appear after clicking "Update Model List", the CLI may not be properly configured or accessible

8. **Model validation warnings**
   - Model validation shows warnings when no models are available from CLI
   - This is expected behavior when CLI is not accessible or not configured
   - Use the "Update Model List" button to fetch available models
   - Ensure Codex CLI is properly installed and configured with API keys

### Debug Information

- Check build logs for detailed error messages
- Use the "Test Codex CLI" button in job-level configuration to test node-specific CLI accessibility
- Review analysis content and prompts for quality
- Verify that the Codex CLI is accessible on the specific node where your job runs

## Development

### Building the Plugin

```bash
# Clean and build the HPI package
mvn clean package

# (Optional) compile only
mvn compile

# HPI output: target/codex-analysis.hpi
```

### Running Tests

```bash
mvn test

# Quiet unit tests (skip integration tests)
mvn -q -DskipITs=true test
```

### Install the Plugin

Choose one:

- UI: Manage Jenkins → Manage Plugins → Advanced → Upload Plugin → select `target/codex-analysis.hpi` → Upload → Restart Jenkins
- Filesystem: copy `target/codex-analysis.hpi` to `$JENKINS_HOME/plugins/codex-analysis.hpi` and restart Jenkins

### Upgrade the Plugin

Use the Maven Versions Plugin to update `pom.xml`:

```bash
# Set a specific release version
mvn versions:set -DnewVersion=1.0.0

# Or bump snapshot
mvn versions:set -DnewVersion=1.0.1-SNAPSHOT

# Write changes
mvn versions:commit
```

Optional helpers:

```bash
# Show available dependency updates
mvn versions:display-dependency-updates

# Try latest compatible releases (review carefully)
mvn versions:use-latest-releases
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Submit a pull request

## Support

- **Documentation**: [Plugin Help](src/main/resources/help.html)
- **Issues**: Create an issue in the repository
- **Codex CLI**: [https://github.com/openai/codex](https://github.com/openai/codex)
- **Configuration Guide**: [https://github.com/openai/codex/blob/main/docs/config.md](https://github.com/openai/codex/blob/main/docs/config.md)

### Getting Help

1. **Configuration Issues**: Use the job-level "Test Codex CLI" button to diagnose node-specific problems
2. **Global vs Job-Level**: Remember that job-level settings override global settings when enabled
3. **Node Binding**: Always test CLI accessibility at the job level to ensure proper node binding
4. **Fallback Behavior**: Empty job-level settings will use global defaults

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## Reference

[codex-config](https://github.com/openai/codex/blob/main/docs/config.md)
