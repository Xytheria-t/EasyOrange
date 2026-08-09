FROM docker.elastic.co/elasticsearch/elasticsearch:9.2.8

# IK 分词插件。官方 GitHub release 资产不稳定，改用 InfiniLabs 官方 CDN 的 stable 渠道。
# 版本与 elasticsearch-java 9.2.8（Boot 4.0.7 / Spring Data ES 6.0.6 管理）严格对齐，杜绝客户端/服务端大版本不兼容。
RUN /usr/share/elasticsearch/bin/elasticsearch-plugin install --batch \
    https://release.infinilabs.com/analysis-ik/stable/elasticsearch-analysis-ik-9.2.8.zip

# 单节点 + replicas:1 时集群长期 yellow（副本无法在单节点分配），只认 green 会永远不健康
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=5 \
    CMD curl -s http://localhost:9200/_cluster/health | grep -qE '"status":"(green|yellow)"' || exit 1
