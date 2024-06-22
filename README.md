## 欢迎使用 TPM

这是一个用于在服务器中给予普通玩家 TP 权限，但不至于让玩家随意 TP 别人的模组。

## 运行环境

+ `minecraft` ~ 1.20.1
+ `fabric loader` >= 0.15.11
+ `fabric api` *
+ `fabric language koltin` *
+ `java` >= 17（最好是 21）

## 指令

参数解释（尖括号`<>`表示必填，方括号`[]`表示可选）：

+ `level`: 维度名称（比如：`minecraft:overworld`）
+ `pos`: 坐标（比如：`1 5 6`）
+ `player`: 玩家名称
+ `force`: 是否强制请求（可选值：`true`/`false`）

指令列表：

+ `tpp`: 传送到当前世界的主城/出生点
+ `tpp <level>`: 传送到指定世界的主城/出生点
+ `tpp <pos>`: 传送到当前世界的指定坐标
+ `tpp <level> <pos>`: 传送到指定世界的指定坐标
+ `tpa <player> [force]`: 请求传送到指定玩家，`force` 为 `true` 时自动取消已发起的传送请求
+ `tphere <player> [force]`: 请求将指定玩家传送到自己，`force` 为 `true` 时自动取消已发起的传送请求
+ `tphome`: 传送到家
+ `tpspawn`: 传送到重生点
+ `tpconfig set`: 调整设置
+ `tpconfig get`: 读取设置
+ `tpaccept [player]`: 接受指定玩家/所有人发起的传送请求（对于 `tphere`，必须携带 `player` 参数）
+ `tpreject [player]`: 拒绝指定玩具/所有人发起的传送请求

可调参数列表（通过 `tpconfig` 调整）：

+ `home`: 无权限要求，用于设置家的坐标<br/>
    调用方式：`tpconfig set home`
+ `auto_reject`: 无权限要求，用于开启或关闭自动拒绝传送请求的功能<br/>
    调用方式：`tpconfig set auto_reject true/false`
+ `auto_accept`: 无权限要求，用于开启或关闭自动接受传送请求的功能<br/>
    调用方式：`tpconfig set auto_accept true/false`<br/>
    注：不支持自动接受 `tphere` 发起的传送请求
+ `main`: 要求 3 级管理员权限，用于设置指定世界的主城坐标<br/>
    调用方式：`tpconfig set main [level] [pos]`<br/>
    注：`level` 和 `pos` 要么都填，要么都不填，不填表示设置到当前位置