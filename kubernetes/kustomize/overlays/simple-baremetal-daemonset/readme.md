# DaemonSet deployment on baremetal kubernetes cluster

here we deploy 1 otoroshi instance on each kubernetes node (with the `otoroshi-kind: instance` label) with redis persistance. 

The otoroshi instances are exposed using `hostPort` so you'll have to add a loadbalancer in front of your kubernetes nodes to route external traffic (TCP) to your otoroshi instances. You'll also have to configure your DNS to route otoroshi domain names to the loadbalancer itself

## NGINX config. example

```
stream {

  upstream back_http_nodes {
    zone back_http_nodes 64k;
    server 10.2.2.40:41080 max_fails=1;
    server 10.2.2.41:41080 max_fails=1;
    server 10.2.2.42:41080 max_fails=1;
  }

  upstream back_https_nodes {
    zone back_https_nodes 64k;
    server 10.2.2.40:41443 max_fails=1;
    server 10.2.2.41:41443 max_fails=1;
    server 10.2.2.42:41443 max_fails=1;
  }

  server {
    listen     80;
    proxy_pass back_http_nodes;
    health_check;
  }

  server {
    listen     443;
    proxy_pass back_https_nodes;
    health_check;
  }
  
}
```

## HAProxy config. example

```
frontend front_nodes_http
    bind *:80
    mode tcp
    default_backend back_http_nodes
    timeout client          1m

frontend front_nodes_https
    bind *:443
    mode tcp
    default_backend back_https_nodes
    timeout client          1m

backend back_http_nodes
    mode tcp
    balance roundrobin
    server kubernetes-node1 10.2.2.40:41080
    server kubernetes-node2 10.2.2.41:41080
    server kubernetes-node3 10.2.2.42:41080
    timeout connect        10s
    timeout server          1m

backend back_https_nodes
    mode tcp
    balance roundrobin
    server kubernetes-node1 10.2.2.40:41443
    server kubernetes-node2 10.2.2.41:41443
    server kubernetes-node3 10.2.2.42:41443
    timeout connect        10s
    timeout server          1m
```

## DNS config. example

if your loadbalancer is at ip address 10.2.2.50

```
otoroshi.your.otoroshi.domain      IN A 10.2.2.50
otoroshi-api.your.otoroshi.domain  IN A 10.2.2.50
privateapps.your.otoroshi.domain   IN A 10.2.2.50
api1.another.domain                IN A 10.2.2.50
api2.another.domain                IN A 10.2.2.50
*.api.the.api.domain               IN A 10.2.2.50
```