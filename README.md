- Mysql数据库：
    - 添加指定ip的访问权限：数据库中的mysql表
    - 开启/关闭/重启数据库：**systemctl start/stop/restart mariadb.service**
        ```mysql
        grant all privileges on *.* to root@'指定的ip' identified by '验证的密码';
        flush privileges;
        ```
- Redis数据库：
    - 存放路径：**/home/admin/redis/redis-5.0.4**
    - 配置文件路径：**/home/admin/redis/redis-5.0.4/redis.conf**
        - 密码为配置文件中的 requirepass行（自行搜索）
    - PATH配置文件路径：**~/.bash_profile**
    ```shell
        开启server服务：redis-server redis.conf &（若不指定配置文件则无密码）
        启动client：redis-cli
            开启后输入语句：auth 密码（若不输入则无法正常使用）
    ```
    
- RocketMQ:
    - 存放路径：**/home/admin/rocketmq/rocketmq-all-4.7.1-bin-release**
    - 修改了bin下的mqbroker.xml和runbroker.sh的jvm参数
    - 启动nameserver:**nohup sh bin/mqnamesrv &**
    - 启动broker:**nohup sh bin/mqbroker -n localhost:9876 -c conf/broker.conf printConfigItem & **
    - 停止broker:**sh bin/mqshutdown broker**
    - 停止nameserver：**sh bin/mqshutdown namesrv**
    - 查看nameserver日志：**tail -f ~/logs/rocketmqlogs/namesrv.log**
    - 查看broker日志：**tail -f ~/logs/rocketmqlogs/broker.log**
    - 创建topic: **./mqadmin updateTopic -n localhost:9876 -t stock（topic名字） -c DefaultCluster**(bin目录下)
 
- nginx:
    - openresty:
        - tar存放路径：**/tmp/openresty-1.13.6.2**
        - 安装路径：**/usr/local/openresty**
        - 配置文件路径：**/usr/local/openresty/nginx/conf/nginx.conf**
        - 前端资源文件路径：**/usr/local/openresty/nginx/html/resources/**
        - 启动nginx：**sbin/nginx -c conf/nginx.conf**（在/usr/local/openresty/nginx目录）
        - 修改配置文件后无缝开启nginx：**sbin/nginx -s reload**（在/usr/local/openresty/nginx目录）
        - nginx的缓存：**/usr/local/openresty/nginx/tmp_cache**
        - lua脚本存放位置：**/usr/local/openresty/lua**

- java:
    - rpm存放路径：**/home/admin/java**
    - 安装路径：**/usr/java**
    - PATH配置路径：**~/.bash_profile**

- 项目：
    - 项目存放路径：**/home/admin/Project/miaosha**