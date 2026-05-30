FROM quay.io/keycloak/keycloak:24.0

COPY init-keycloak.sh /opt/keycloak/init-keycloak.sh

RUN chmod +x /opt/keycloak/init-keycloak.sh

ENTRYPOINT ["/opt/keycloak/bin/kc.sh", "start-dev"]