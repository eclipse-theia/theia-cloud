# build stage
FROM node:16-alpine as build-stage
WORKDIR /app
COPY node/configs ./configs/
COPY node/tsconfig.json node/package*.json ./
COPY node/landing-page/. ./landing-page/
RUN npm ci
RUN npm run build:nolint -w landing-page

# production stage
FROM nginx:stable-alpine as production-stage
COPY --from=build-stage /app/dist /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]