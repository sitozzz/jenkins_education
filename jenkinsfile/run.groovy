
pipeline {
    agent{node('master')}
    stages {
        stage('Clean workspace & dowload dist') {
            steps {
                script {
                    cleanWs()
                    withCredentials([
                        usernamePassword(credentialsId: 'srv_sudo',
                        usernameVariable: 'username',
                        passwordVariable: 'password')
                    ]) {
                        try {
                            sh "echo '${password}' | sudo -S docker stop isng"
                            sh "echo '${password}' | sudo -S docker container rm isng"
                        } catch (Exception e) {
                            print 'container not exist, skip clean'
                        }
                    }
                }
                script {
                    echo 'Update from repository'
                    checkout([$class                           : 'GitSCM',
                              branches                         : [[name: '*/master']],
                              doGenerateSubmoduleConfigurations: false,
                              extensions                       : [[$class           : 'RelativeTargetDirectory',
                                                                   relativeTargetDir: 'auto']],
                              submoduleCfg                     : [],
                              userRemoteConfigs                : [[credentialsId: 'IvanSitnikovGit', url: 'https://github.com/sitozzz/jenkins_education.git']]])
                }
            }
        }
        stage ('Build & run docker image'){
            steps{
                script{
                     withCredentials([
                        usernamePassword(credentialsId: 'srv_sudo',
                        usernameVariable: 'username',
                        passwordVariable: 'password')
                    ]) {

                        sh "echo '${password}' | sudo -S docker build ${WORKSPACE}/auto -t ivan_sitnikov_nginx "
                        sh "echo '${password}' | sudo -S docker run -d -p 8123:80 --name isng ivan_sitnikov_nginx"
                    }
                }
            }
        }
        stage ('Get stats & write to file'){
            steps{
                script{
                    withCredentials([
                        usernamePassword(credentialsId: 'srv_sudo',
                        usernameVariable: 'username',
                        passwordVariable: 'password')
                    ]) {
                        try {
                            sh "truncate -s 0 ${WORKSPACE}/stats.txt"
                        } catch (Exception e) {
                            print 'file exist'
                        }
                        sh "echo '${password}' | sudo -S docker exec -t isng df -h >> ${WORKSPACE}/stats.txt"
                        sh "echo '${password}' | sudo -S docker exec -t isng -c 'top -n 1 -b' >> ${WORKSPACE}/stats.txt"
                    }
                }
            }
        }
        
    }

    
}
