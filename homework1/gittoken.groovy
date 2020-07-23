rm -def k8slabel = "jenkins-pipeline-${UUID.randomUUID().toString()}"
def slavePodTemplate = """
      metadata:
        labels:
          k8s-label: ${k8slabel}
        annotations:
          jenkinsjoblabel: ${env.JOB_NAME}-${env.BUILD_NUMBER}
      spec:
        affinity:
          podAntiAffinity:
            requiredDuringSchedulingIgnoredDuringExecution:
            - labelSelector:
                matchExpressions:
                - key: component
                  operator: In
                  values:
                  - jenkins-jenkins-master
              topologyKey: "kubernetes.io/hostname"
        containers:
        - name: github
          image: fuchicorp/buildtools:latest
          imagePullPolicy: IfNotPresent
          command:
          - cat
          tty: true
          volumeMounts:
            - mountPath: /var/run/docker.sock
              name: docker-sock
        - name: docker
          image: docker:latest
          imagePullPolicy: IfNotPresent
          command:
          - cat
          tty: true
          volumeMounts:
            - mountPath: /var/run/docker.sock
              name: docker-sock
        serviceAccountName: default
        securityContext:
          runAsUser: 0
          fsGroup: 0
        volumes:
          - name: docker-sock
            hostPath:
              path: /var/run/docker.sock
    """
    properties([
      parameters
      ([choice(choices: ['mary', 'sam', 'anna', 'bob'], 
      description: 'Please select one user', name: 'github_user')
      ])
      ])
    podTemplate(name: k8slabel, label: k8slabel, yaml: slavePodTemplate, showRawYaml: false) {
      node(k8slabel){
        withCredentials([string(credentialsId: 'github-token', variable: 'GIT_TOKEN')]) {
        stage("Check Creds") {
           container("github"){
           sh  'curl -H "Authorization: token $GIT_TOKEN" -X GET "https://api.github.com/users" -I |grep "HTTP/1.1 200 OK" '
          }
        }
        stage("Get User"){
           container("github"){
             sh  "curl -H \"Authorization: token $GIT_TOKEN \" -X GET 'https://api.github.com/users/${github_user}' "
          }
        }
        }
      }
    }