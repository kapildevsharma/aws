# aws
# How to Use AWS Services With Sprint Boot
In properties file update the AWS Configuration
like 
	aws.accessKeyId=
	aws.secretAccessKey=
	aws.region=eu-north-1

	Run below command into every public EC2 to connect with ElastiCache for Redis
	sudo apt-get update
	Install mysql client : 
		sudo apt install mysql-client
	Change normal user to root user : 
		sudo  su -
	Connect mysql server: 
		mysql -h my-rds-database.clw2g6uqcig9.eu-north-1.rds.amazonaws.com -u admin -p
		
	Install redis cli to connect with redis server:
		sudo apt install redis-tools
		
	Connect with redis server from ec2 instance: 
		redis-cli --tls -h project-redis-cache-6rgo2n.serverless.eun1.cache.amazonaws.com -p 6379
	Install CURL software : 
		 sudo apt install unzip curl -y
	Install git :
		apt install git
	Install Java:
		sudo apt install default-jdk -y
		java -version
	Install Maven : 
		sudo apt install maven
			Example : mvn spring-boot:run

	Download file from S3 bucket into EC2 instance:
		aws s3 cp s3://project-kdsbucket-cloudbunner/spring-boot-sqs-0.0.1-SNAPSHOT.jar /home/ubuntu/ec2-user/
	Change the file permission for executable permission
		chmod +x spring-boot-sqs-0.0.1-SNAPSHOT.jar 
	Run jar file to start spring boot application
		 java -jar spring-boot-sqs-0.0.1-SNAPSHOT.jar
		 
		Testing application so call rest api by using CRUL 
			curl http://localhost:8080/api/aws/person/1
			curl "http://localhost:8080/cache-set?key=user&value=myValue"


redis-cli --tls -h project-redis-cache-6rgo2n.serverless.eun1.cache.amazonaws.com -p 6379
mysql -h rds-database.clw2g6uqcig9.eu-north-1.rds.amazonaws.com -u admin -p


