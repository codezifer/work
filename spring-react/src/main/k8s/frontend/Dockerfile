FROM node:alpine AS builder
ARG APP_DIR=../../app
RUN mkdir /app
COPY ${APP_DIR} /app/

WORKDIR /app
RUN npm install \
    && npm run build:prod
COPY . .

##

FROM nginx:stable-alpine
COPY --from=builder /app/dist/ /usr/share/nginx/html/
