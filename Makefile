.DEFAULT_GOAL := all


native:
	- echo building container
	docker run -v $(HOME)/.m2:/root/.m2 -v "`pwd`:/opt/graalvm/" -t graalvm mvn package

container:
	docker-compose -f docker-compose.build.yml build app
	docker run -it app /app --help

prepare:
	- echo build graalvm
	docker-compose -f docker-compose.build.yml build graalvm

configure-native:
	docker run -p4567:4567 -v $(HOME)/.m2:/root/.m2 -v "`pwd`:/opt/graalvm/" -t graalvm \
		java -agentlib:native-image-agent=config-output-dir=./src/main/resources/META-INF/ \
		-cp completejarpath1:completejarpath2:verylong:canbeveryverylong
		ineat.demo.AppMain

clean:
	docker run -v $(HOME)/.m2:/root/.m2 -v "`pwd`:/opt/graalvm/" -t graalvm mvn clean



all: build-firewall build-nginx build-site native save-docker

