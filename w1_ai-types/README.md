# w1_ai-types

## 模块定位

`w1_ai-types` 是旧工程遗留的公共类型模块，目前处于待退役状态。它曾保存常量、响应码和异常，但这些职责已经迁移到 `w1_ai-shared`。

## 目录结构

```text
cn.winddol.ai.types
├─ common/     旧全局常量
├─ enums/      旧响应码
└─ exception/  旧业务异常
```

## 文件说明

| 文件 | 当前作用 | 目标位置 |
| --- | --- | --- |
| `common/Constants.java` | 兼容旧代码的常量定义。 | `w1_ai-shared/common` |
| `enums/ResponseCode.java` | 兼容旧代码的响应码。 | `w1_ai-shared/enums` |
| `exception/AppException.java` | 兼容旧代码的异常类型。 | `w1_ai-shared/exception` |

## 迁移规则

- 新代码禁止依赖 `cn.winddol.ai.types.*`。
- 修改旧代码时，应同步改用 `cn.winddol.ai.shared.*`。
- 当全仓库不再引用本模块后，从父 POM 和依赖树中删除。

