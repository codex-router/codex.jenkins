// ============================================================================
// Codex Analysis Plugin - Pipeline Examples
// ============================================================================
// This file contains example Jenkins Pipeline scripts demonstrating
// various ways to use the Codex Analysis Plugin.

// ============================================================================
// Example 1: Basic Build Analysis
// ============================================================================
pipeline {
    agent any

    stages {
        stage('Build') {
            steps {
                sh 'mvn clean compile'

                // Analyze build output
                codexAnalysis(
                    content: 'Maven build completed successfully',
                    analysisType: 'build_analysis',
                    prompt: 'Analyze this Maven build and suggest optimizations'
                )
            }
        }
    }
}

// ============================================================================
// Example 2: Comprehensive CI/CD Pipeline with Analysis
// ============================================================================
pipeline {
    agent any

    environment {
        // Set your API keys here or in Jenkins credentials
        LITELLM_API_KEY = credentials('litellm-api-key')
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                sh 'mvn clean compile'

                // Analyze build process
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

                // Analyze test results
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

                // Analyze security scan results
                codexAnalysis(
                    content: 'Security dependency check completed',
                    analysisType: 'security_analysis',
                    includeContext: true
                )
            }
        }

        stage('Quality Check') {
            steps {
                sh 'mvn checkstyle:check'
                sh 'mvn spotbugs:check'

                // Analyze code quality
                codexAnalysis(
                    content: 'Code quality checks completed',
                    analysisType: 'quality_analysis',
                    prompt: 'Analyze code quality metrics and suggest improvements'
                )
            }
        }

        stage('Deploy') {
            when {
                branch 'main'
            }
            steps {
                sh 'mvn deploy'

                // Analyze deployment
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
            // Always analyze the overall pipeline execution
            codexAnalysis(
                content: 'Pipeline execution completed',
                analysisType: 'general',
                prompt: 'Provide a comprehensive analysis of this CI/CD pipeline execution'
            )
        }
        failure {
            // Analyze failures
            codexAnalysis(
                content: 'Pipeline failed - analyze the failure',
                analysisType: 'general',
                prompt: 'Analyze this pipeline failure and provide troubleshooting steps'
            )
        }
    }
}

// ============================================================================
// Example 3: Advanced Analysis with Custom Parameters
// ============================================================================
pipeline {
    agent any

    stages {
        stage('Advanced Analysis') {
            steps {
                script {
                    // Get build output
                    def buildOutput = sh(
                        script: 'mvn clean compile 2>&1',
                        returnStdout: true
                    ).trim()

                    // Analyze with custom parameters
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

                    // Use the analysis result
                    echo "Analysis result: ${analysisResult}"

                    // Parse and act on analysis
                    if (analysisResult.contains('critical')) {
                        error 'Critical issues found in analysis'
                    }
                }
            }
        }
    }
}

// ============================================================================
// Example 4: Conditional Analysis Based on Build Results
// ============================================================================
pipeline {
    agent any

    stages {
        stage('Build and Analyze') {
            steps {
                script {
                    try {
                        sh 'mvn clean compile'

                        // Success analysis
                        codexAnalysis(
                            content: 'Build succeeded',
                            analysisType: 'build_analysis',
                            prompt: 'Analyze this successful build and suggest optimizations'
                        )

                    } catch (Exception e) {
                        // Failure analysis
                        codexAnalysis(
                            content: "Build failed: ${e.getMessage()}",
                            analysisType: 'build_analysis',
                            prompt: 'Analyze this build failure and provide solutions'
                        )
                        throw e
                    }
                }
            }
        }
    }
}

// ============================================================================
// Example 5: Multi-Environment Analysis
// ============================================================================
pipeline {
    agent any

    stages {
        stage('Build') {
            steps {
                sh 'mvn clean compile'
            }
        }

        stage('Test') {
            parallel {
                stage('Unit Tests') {
                    steps {
                        sh 'mvn test'
                        codexAnalysis(
                            content: 'Unit tests completed',
                            analysisType: 'test_analysis'
                        )
                    }
                }
                stage('Integration Tests') {
                    steps {
                        sh 'mvn verify -Pintegration'
                        codexAnalysis(
                            content: 'Integration tests completed',
                            analysisType: 'test_analysis'
                        )
                    }
                }
            }
        }

        stage('Deploy to Environments') {
            parallel {
                stage('Deploy to Staging') {
                    steps {
                        sh 'mvn deploy -Pstaging'
                        codexAnalysis(
                            content: 'Staging deployment completed',
                            analysisType: 'deployment_analysis',
                            prompt: 'Analyze staging deployment and check for issues'
                        )
                    }
                }
                stage('Deploy to Production') {
                    when {
                        branch 'main'
                    }
                    steps {
                        sh 'mvn deploy -Pproduction'
                        codexAnalysis(
                            content: 'Production deployment completed',
                            analysisType: 'deployment_analysis',
                            prompt: 'Analyze production deployment and verify security'
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// Example 6: Performance Analysis Pipeline
// ============================================================================
pipeline {
    agent any

    stages {
        stage('Build') {
            steps {
                sh 'mvn clean compile'
            }
        }

        stage('Performance Test') {
            steps {
                sh 'mvn test -Dtest=PerformanceTest'

                // Analyze performance results
                codexAnalysis(
                    content: 'Performance tests completed',
                    analysisType: 'performance_analysis',
                    prompt: 'Analyze performance test results and identify bottlenecks'
                )
            }
        }

        stage('Load Test') {
            steps {
                sh 'mvn test -Dtest=LoadTest'

                // Analyze load test results
                codexAnalysis(
                    content: 'Load tests completed',
                    analysisType: 'performance_analysis',
                    prompt: 'Analyze load test results and suggest optimizations'
                )
            }
        }
    }
}

// ============================================================================
// Example 7: Security-Focused Pipeline
// ============================================================================
pipeline {
    agent any

    stages {
        stage('Security Scan') {
            steps {
                sh 'mvn dependency:check'
                sh 'mvn spotbugs:check'
                sh 'mvn checkstyle:check'

                // Comprehensive security analysis
                codexAnalysis(
                    content: 'Security scans completed',
                    analysisType: 'security_analysis',
                    prompt: 'Perform comprehensive security analysis and identify vulnerabilities'
                )
            }
        }

        stage('Vulnerability Assessment') {
            steps {
                sh 'mvn org.owasp:dependency-check-maven:check'

                // Analyze vulnerability scan results
                codexAnalysis(
                    content: 'Vulnerability assessment completed',
                    analysisType: 'security_analysis',
                    prompt: 'Analyze vulnerability scan results and prioritize fixes'
                )
            }
        }
    }
}

// ============================================================================
// Example 8: Custom Analysis with Script Output
// ============================================================================
pipeline {
    agent any

    stages {
        stage('Custom Analysis') {
            steps {
                script {
                    // Run custom script
                    def scriptOutput = sh(
                        script: '''
                            echo "Custom analysis script"
                            echo "Checking system resources..."
                            df -h
                            free -m
                            echo "Analysis complete"
                        ''',
                        returnStdout: true
                    ).trim()

                    // Analyze script output
                    codexAnalysis(
                        content: scriptOutput,
                        analysisType: 'general',
                        prompt: 'Analyze this system resource check and provide recommendations'
                    )
                }
            }
        }
    }
}
