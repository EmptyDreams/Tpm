## 欢迎使用 TPM

这是一个用于在服务器中给予普通玩家 TP 权限，但不至于让玩家随意 TP 别人的模组。

## 关于版本号

本模组版本号由 `A.B.C` 组成：

+ `C` 变动时说明为漏洞修复或轻微修改，请尽量跟随更新。
+ `B` 变动时说明添加了新的功能或对已有功能进行了轻微修改，同时可能附带漏洞修复，请尽量跟随更新。
+ `A` 变动时表明对已有功能做出了大量修改，导致模组使用方法发生重大变化，同时可能附带漏洞修复，可以酌情更新，更新后请务必充新阅读文档。

注意：`A.B.C` 用于标明模组用法的变化，本模组并未设计为为其它模组提供 API，请勿将本模组作为您的模组前置，若需要修改本模组的功能，请在遵守开源协议的前提下直接修改模组代码并发布属于您的模组，否则您将需要自己注意我在代码层面的修改。

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

+ `tphelp`: 获取帮助信息
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
+ `tpm set`: 修改服务器指定设置
+ `tpm get`: 读取服务器指定设置
+ `tpm set person <player>`: 修改指定玩家的指定配置（要求 3 级管理员）
+ `tpm reload`: 重新加载配置文件
+ `tpaccept [player]`: 接受指定玩家/所有人发起的传送请求（对于 `tphere`，必须携带 `player` 参数）
+ `tpreject [player]`: 拒绝指定玩具/所有人发起的传送请求

普通用户可调参数列表（通过 `tpconfig` 调整）：

+ `home`: 用于设置家的坐标<br/>
    调用方式：`tpconfig set home`
+ `auto_reject`: 用于开启或关闭自动拒绝传送请求的功能<br/>
    调用方式：`tpconfig set auto_reject true/false`
+ `auto_accept`: 用于开启或关闭自动接受传送请求的功能<br/>
    调用方式：`tpconfig set auto_accept true/false`<br/>
    注：不支持自动接受 `tphere` 发起的传送请求

管理员可调参数列表（通过`tpm`）

+ `main`: 用于设置指定世界的主城坐标<br/>
  调用方式：`tpm set main [level] [pos]`<br/>
  注：`level` 和 `pos` 要么都填，要么都不填，不填表示设置到当前位置

## 配置文件

配置存储在 `/config/tpm.json` 文件中，若文件不存在将会自动生成，其中会包含一个格式范例（建议修改时将其删除），配置文件示例如下：

```json
{
  "version": 1,
  "default": {
    "auto_accept": [
      {
        "regex": "^bot_",
        "ignoreCase": true,
        "value": true
      }
    ],
    "auto_reject": [
      {
        "regex": ".*",
        "value": false
      }
    ],
    "home": [
      {
        "regex": ".*",
        "value": ["minecraft:overworld", 114514, 75.3, 415411]
      }
    ]
  }
}
```

其中每一项数组中越靠前的优先级越大，`regex` 使用正则表达式匹配玩家名称，添加 `ignoreCase` 可以让正则表达式忽略大小写（默认不忽略）。如果一个玩家 `auto_reject` 和 `auto_accept` 均为 `true`，则 `auto_reject` 生效。

所有布尔类型的全局缺省值均为 `false`，非基本类型的全局缺省值均为 `null`。上述 `json` 仅为范例，实际使用中建议删除与全局缺省值相同的配置（会影响性能）。