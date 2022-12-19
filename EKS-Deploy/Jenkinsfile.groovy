pipeline{
    agent {label 'ansible'}
    environment {
        PATH = "$PATH:/usr/share/apache-maven/bin"
        DOCKER_TAG = "${env.BUILD_ID}"
        registory="513704694763.dkr.ecr.us-west-2.amazonaws.com/docker-container"
    }
    parameters {
        string(name: 'RELEASE_REPO', defaultValue: "libs-release", description: 'Repo for releses')
        string(name: 'SNAPSHOT_REPO', defaultValue: "libs-snapshot", description: 'Repo for snapshot releses')
        
    }
    stages{
        stage('scm'){
            steps{
                git branch: 'main', 
                credentialsId: 'github', 
                url: 'https://github.com/manikumar9951/test'
            }
        }
        stage('Artifactory-Configuration') {
            steps {
                rtMavenDeployer (
                    id: 'spc-deployer',
                    serverId: 'jfrog-artifactory',
                    releaseRepo: "${params.RELEASE_REPO}",
                    snapshotRepo: "${params.SNAPSHOT_REPO}"
                    
                )
            }
        }
        stage('Upload artifacts to jfrog') {
            steps {
                rtMavenRun (
                    // Tool name from Jenkins configuration.
                    tool: 'MVN_BUILD',
                    pom: 'pom.xml',
                    goals: 'install',
                    // Maven options.
                    deployerId: 'spc-deployer'
                )
            }
        }
        stage('Docker-Build'){
            steps{
                sh 'docker build -t dockerfile:Helloworld-${DOCKER_TAG} .'
            }
        }
        stage('Push image to ECR'){
            steps{
                script{
                    sh 'aws ecr get-login-password --region us-west-2 | docker login --username AWS --password-stdin 513704694763.dkr.ecr.us-west-2.amazonaws.com'
                    sh 'docker tag dockerfile:Helloworld-${DOCKER_TAG} 513704694763.dkr.ecr.us-west-2.amazonaws.com/docker-container:Helloworld-${DOCKER_TAG}'
                    sh 'docker push 513704694763.dkr.ecr.us-west-2.amazonaws.com/docker-container:Helloworld-${DOCKER_TAG}'
                }
            }
        }
        stage('Deploy ECR image to eks deployment'){
            steps{
                script{
                    sh '/root/bin/kubectl apply -f deploy.yaml'
                    sh '/root/bin/kubectl apply -f service.yaml'
                    sh '/root/bin/kubectl get services'
                }
            }
        }
    }
}
