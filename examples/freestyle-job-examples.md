# Freestyle Job Examples

This document provides examples of how to use the Codex Analysis Plugin with Jenkins Freestyle jobs.

## Example 1: Basic Build Analysis

### Job Configuration
1. Create a new Freestyle job
2. Add build steps:
   - **Build step**: Execute shell command
     ```bash
     mvn clean compile
     ```
   - **Build step**: Codex Analysis
     - Content to Analyze: `Maven build completed successfully`
     - Analysis Type: `build_analysis`
     - Custom Prompt: `Analyze this Maven build and suggest optimizations`
     - Include Build Context: ✓ (checked)
     - Fail on Error: ✗ (unchecked)

### Expected Result
The job will run the Maven build and then analyze the build process, providing insights and optimization suggestions.

## Example 2: Test Analysis with Custom Model

### Job Configuration
1. Create a new Freestyle job
2. Add build steps:
   - **Build step**: Execute shell command
     ```bash
     mvn test
     ```
   - **Build step**: Codex Analysis
     - Content to Analyze: `Unit tests executed successfully`
     - Analysis Type: `test_analysis`
     - Model: `gpt-4`
     - Timeout: `180`
     - Additional Parameters:
       ```
       temperature=0.7
       max_tokens=2000
       include_code=true
       ```
     - Include Build Context: ✓ (checked)

### Expected Result
The job will run tests and then perform detailed test analysis using GPT-4 with custom parameters.

## Example 3: Security Analysis Pipeline

### Job Configuration
1. Create a new Freestyle job
2. Add build steps:
   - **Build step**: Execute shell command
     ```bash
     mvn dependency:check
     mvn spotbugs:check
     ```
   - **Build step**: Codex Analysis
     - Content to Analyze: `Security scans completed`
     - Analysis Type: `security_analysis`
     - Custom Prompt: `Perform comprehensive security analysis and identify vulnerabilities`
     - Include Build Context: ✓ (checked)
     - Fail on Error: ✓ (checked)

### Expected Result
The job will run security scans and fail if critical security issues are found during analysis.

## Example 4: Performance Analysis

### Job Configuration
1. Create a new Freestyle job
2. Add build steps:
   - **Build step**: Execute shell command
     ```bash
     mvn test -Dtest=PerformanceTest
     ```
   - **Build step**: Codex Analysis
     - Content to Analyze: `Performance tests completed`
     - Analysis Type: `performance_analysis`
     - Custom Prompt: `Analyze performance test results and identify bottlenecks`
     - Include Build Context: ✓ (checked)

### Expected Result
The job will run performance tests and analyze the results for performance bottlenecks and optimization opportunities.

## Example 5: Quality Analysis with Custom Content

### Job Configuration
1. Create a new Freestyle job
2. Add build steps:
   - **Build step**: Execute shell command
     ```bash
     mvn checkstyle:check
     mvn spotbugs:check
     mvn pmd:check
     ```
   - **Build step**: Codex Analysis
     - Content to Analyze:
       ```
       Code quality checks completed:
       - Checkstyle: 0 errors, 5 warnings
       - SpotBugs: 2 medium priority issues found
       - PMD: 1 high priority issue found
       ```
     - Analysis Type: `quality_analysis`
     - Custom Prompt: `Analyze code quality metrics and suggest improvements`
     - Include Build Context: ✓ (checked)

### Expected Result
The job will run quality checks and provide detailed analysis of code quality issues with improvement suggestions.

## Example 6: Deployment Analysis

### Job Configuration
1. Create a new Freestyle job
2. Add build steps:
   - **Build step**: Execute shell command
     ```bash
     mvn clean package
     docker build -t myapp:latest .
     docker push myapp:latest
     ```
   - **Build step**: Codex Analysis
     - Content to Analyze: `Application deployed to container registry`
     - Analysis Type: `deployment_analysis`
     - Custom Prompt: `Analyze deployment process and verify best practices`
     - Include Build Context: ✓ (checked)

### Expected Result
The job will build and deploy the application, then analyze the deployment process for best practices and potential issues.

## Example 7: Multi-Step Analysis

### Job Configuration
1. Create a new Freestyle job
2. Add build steps:
   - **Build step**: Execute shell command
     ```bash
     mvn clean compile
     ```
   - **Build step**: Codex Analysis
     - Content to Analyze: `Build completed`
     - Analysis Type: `build_analysis`
     - Include Build Context: ✓ (checked)

   - **Build step**: Execute shell command
     ```bash
     mvn test
     ```
   - **Build step**: Codex Analysis
     - Content to Analyze: `Tests completed`
     - Analysis Type: `test_analysis`
     - Include Build Context: ✓ (checked)

   - **Build step**: Execute shell command
     ```bash
     mvn package
     ```
   - **Build step**: Codex Analysis
     - Content to Analyze: `Packaging completed`
     - Analysis Type: `general`
     - Custom Prompt: `Provide overall analysis of the build process`
     - Include Build Context: ✓ (checked)

### Expected Result
The job will perform multiple analysis steps throughout the build process, providing insights at each stage.

## Example 8: Error Analysis

### Job Configuration
1. Create a new Freestyle job
2. Add build steps:
   - **Build step**: Execute shell command
     ```bash
     mvn clean compile || true
     ```
   - **Build step**: Codex Analysis
     - Content to Analyze: `Build may have failed - analyze the output`
     - Analysis Type: `build_analysis`
     - Custom Prompt: `Analyze this build output and provide troubleshooting steps`
     - Include Build Context: ✓ (checked)
     - Fail on Error: ✗ (unchecked)

### Expected Result
The job will attempt to build and then analyze the output regardless of success or failure, providing troubleshooting guidance.

## Configuration Tips

### Environment Variables
Set up environment variables in Jenkins:
- Go to **Manage Jenkins** → **Configure System** → **Global Properties**
- Add environment variables:
  - `LITELLM_API_KEY`: Your LiteLLM API key
  - `CUSTOM_API_TOKEN`: Your custom API token (if using custom MCP servers)

### Build Triggers
Configure appropriate build triggers:
- **Poll SCM**: Check for changes in source code
- **Build periodically**: Run analysis at scheduled intervals
- **Trigger builds remotely**: Allow external triggers

### Post-Build Actions
Add post-build actions for notifications:
- **Email Notification**: Send analysis results via email
- **Slack Notification**: Send results to Slack channels
- **Archive Artifacts**: Save analysis results as artifacts

### Advanced Configuration
- **Timeout Settings**: Adjust timeout based on analysis complexity
- **Model Selection**: Choose appropriate models for different analysis types
- **MCP Servers**: Enable additional capabilities through MCP servers
- **Custom Parameters**: Use additional parameters for fine-tuned analysis

## Troubleshooting

### Common Issues
1. **Codex CLI not found**: Ensure Codex CLI is installed and in PATH
2. **API key errors**: Verify API keys are correctly configured
3. **Timeout errors**: Increase timeout values for complex analyses
4. **Analysis failures**: Check build context and content quality

### Debug Information
- Check build logs for detailed error messages
- Verify Codex CLI configuration
- Test API connectivity
- Review analysis content and prompts

### Best Practices
- Use descriptive content for analysis
- Include relevant build context
- Set appropriate timeouts
- Choose suitable analysis types
- Review and validate analysis results
