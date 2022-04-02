FROM maven:3.6.3-openjdk-11

COPY . .

RUN apt-get update  \
    && apt-get install -y python3 scala apache2 \
    && mvn clean package
# Write simulation result
RUN mvn exec:java -Dexec.mainClass="aws_icl.App" > /var/www/html/index.html

# Configure apache
RUN echo '. /etc/apache2/envvars' > /root/run_apache.sh && \
 echo 'mkdir -p /var/run/apache2' >> /root/run_apache.sh && \
 echo 'mkdir -p /var/lock/apache2' >> /root/run_apache.sh && \ 
 echo '/usr/sbin/apache2 -D FOREGROUND' >> /root/run_apache.sh && \ 
 chmod 755 /root/run_apache.sh

EXPOSE 5000

CMD /root/run_apache.sh


