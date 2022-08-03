# build stage
FROM node:lts-alpine as build-stage
# disable eslint during build
ENV DISABLE_ESLINT_PLUGIN=true
WORKDIR /app
COPY node/package*.json ./
COPY node/common/package.json ./common/package.json
COPY node/try-now-page/package.json ./try-now-page/package.json
RUN npm ci
COPY node/configs/ ./configs/
COPY node/common/ ./common/
COPY node/try-now-page/ ./try-now-page/
RUN npm run build && \
    chmod 644 /app/try-now-page/build/terms.html && \
    chmod 644 /app/try-now-page/build/favicon.ico

# production stage
FROM nginx:stable-alpine as production-stage
COPY --from=build-stage /app/try-now-page/build/ /usr/share/nginx/html/
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]