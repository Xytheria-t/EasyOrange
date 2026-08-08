FROM docker.elastic.co/elasticsearch/elasticsearch:8.17.3

RUN /usr/share/elasticsearch/bin/elasticsearch-plugin install --batch \
    https://github.com/medcl/elasticsearch-analysis-ik/releases/download/v8.17.3/elasticsearch-analysis-ik-8.17.3.zip

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=5 \
    CMD curl -s http://localhost:9200/_cluster/health | grep -q '"status":"green"' || exit 1
