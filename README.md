# nald

mvn clean package -DskipTests=true -P local
docker image build -f ./Dockerfile -t nald.me:5001/docker/main/backend:v1 .
docker push nald.me:5001/docker/main/backend:v1


  