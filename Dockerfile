FROM eclipse-temurin:17-jdk

# Install Chrome
RUN apt-get update && apt-get install -y wget unzip curl gnupg2 \
    && curl -sSL https://dl.google.com/linux/linux_signing_key.pub | apt-key add - \
    && echo "deb [arch=amd64] http://dl.google.com/linux/chrome/deb/ stable main" \
       > /etc/apt/sources.list.d/google-chrome.list \
    && apt-get update && apt-get install -y google-chrome-stable

# Install Chromedriver
RUN CHROME_VERSION=$(google-chrome --version | grep -oP '\d+\.\d+\.\d+') \
    && wget -O /tmp/chromedriver.zip https://chromedriver.storage.googleapis.com/$(curl -s https://chromedriver.storage.googleapis.com/LATEST_RELEASE)/chromedriver_linux64.zip \
    && unzip /tmp/chromedriver.zip -d /usr/local/bin/ && chmod +x /usr/local/bin/chromedriver

# Setup working dir
WORKDIR /app
COPY . .

# Build project
RUN ./mvnw clean package -DskipTests || mvn clean package -DskipTests

EXPOSE 8080

# Run the app
CMD ["java", "-jar", "target/scraper-api-0.0.1-SNAPSHOT.jar"]
