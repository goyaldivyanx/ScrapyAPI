FROM eclipse-temurin:17-jdk

# Install system dependencies and Maven
RUN apt-get update && \
    apt-get install -y wget unzip curl gnupg2 software-properties-common maven

# Install Chrome
RUN curl -sSL https://dl.google.com/linux/linux_signing_key.pub | apt-key add - \
    && echo "deb [arch=amd64] http://dl.google.com/linux/chrome/deb/ stable main" \
    > /etc/apt/sources.list.d/google-chrome.list \
    && apt-get update && apt-get install -y google-chrome-stable

# Install Chromedriver
RUN CHROME_VERSION=$(google-chrome --version | grep -oP '\d+\.\d+\.\d+') \
    && wget -O /tmp/chromedriver.zip https://chromedriver.storage.googleapis.com/$(curl -s https://chromedriver.storage.googleapis.com/LATEST_RELEASE)/chromedriver_linux64.zip \
    && unzip /tmp/chromedriver.zip -d /usr/local/bin/ && chmod +x /usr/local/bin/chromedriver

# Set working directory
WORKDIR /app

# Copy project files
COPY . .

# ✅ Build using Maven (no ./mvnw now)
RUN mvn clean package -DskipTests

# Expose port
EXPOSE 8080

# Run the Spring Boot app
CMD ["java", "-jar", "target/scraper-api-0.0.1-SNAPSHOT.jar"]
