# @q-mono-x/types

QMonoX 框架的共享类型包，负责提供：

- 跨端通用的全局声明与模块声明；
- Web 专属的类型增强；
- Uni 专属的类型增强；
- 可显式引入的基础业务类型。

这个包主要通过 `tsconfig.json` 的 `compilerOptions.types` 使用，也可以通过 `import type` 显式引入
`base` 中的类型。

## 入口职责

| 入口                   | 对应文件         | 职责                                          |
| ---------------------- | ---------------- | --------------------------------------------- |
| `@q-mono-x/types`      | `env/index.d.ts` | 默认入口，只包含跨端通用声明                  |
| `@q-mono-x/types/env`  | `env/index.d.ts` | 通用声明入口，等同默认入口                    |
| `@q-mono-x/types/web`  | `web/index.d.ts` | Web 专属入口，包含通用声明 + Web 类型增强     |
| `@q-mono-x/types/uni`  | `uni/index.d.ts` | Uni 专属入口，包含通用声明 + Uni 类型增强     |
| `@q-mono-x/types/base` | `base.ts`        | 基础业务类型，需要通过 `import type` 显式引入 |

## 目录说明

```txt
packages/types/
  env/
    index.d.ts       # 默认/env 入口
    vue.d.ts         # *.vue 模块声明
    assets.d.ts      # 图片资源模块声明
    unocss.d.ts      # UnoCSS attributify 类型增强
    pinia.d.ts       # Pinia persist 类型增强

  web/
    index.d.ts
    vue-router.d.ts      # vue-router RouteMeta 增强
    ant-design-vue.d.ts  # ant-design-vue 菜单类型增强

  uni/
    index.d.ts
    jsBridge.d.ts                 # jsBridge 模块声明
    vite-plugin-uni-pages.d.ts    # @uni-helper/vite-plugin-uni-pages 类型增强

  base.ts             # 基础通用业务类型
```

## 推荐用法

### Web 应用 / Web 插件

Web 项目需要 `vue-router`、`ant-design-vue` 等 Web 专属增强时，使用：

```jsonc
{
  "compilerOptions": {
    "types": ["vite/client", "@q-mono-x/types/web", "vite-plugin-vue-layouts-next/client"]
  }
}
```

### Uni 应用 / Uni 插件

Uni 项目不要加载 `@q-mono-x/types/web`，避免引入 `vue-router`、`ant-design-vue`
等 Web 专属模块增强。

```jsonc
{
  "compilerOptions": {
    "types": [
      "@dcloudio/types",
      "vite/client",
      "@q-mono-x/types/uni",
      "@uni-helper/uni-types",
      "@uni-helper/vite-plugin-uni-pages"
    ]
  }
}
```

### 通用包 / Node 工具包

只需要 Vue、资源、UnoCSS、Pinia 等跨端通用声明时，使用默认入口或 env 入口：

```jsonc
{
  "compilerOptions": {
    "types": ["node", "vite/client", "@q-mono-x/types"]
  }
}
```

等价于：

```jsonc
{
  "compilerOptions": {
    "types": ["node", "vite/client", "@q-mono-x/types/env"]
  }
}
```

### 只引入某个单模块增强

如果某个包只需要通用声明加一个 Uni 模块增强，可以按子路径精确引入：

```jsonc
{
  "compilerOptions": {
    "types": [
      "@dcloudio/types",
      "vite/client",
      "node",
      "@q-mono-x/types",
      "@q-mono-x/types/uni/jsBridge"
    ]
  }
}
```

也可以按需引入 Web 单模块增强：

```jsonc
{
  "compilerOptions": {
    "types": ["vite/client", "@q-mono-x/types", "@q-mono-x/types/web/vue-router"]
  }
}
```

## 基础业务类型

`base.ts` 中的类型不属于全局声明，需要显式引入：

```ts
import type { QueryParams, PaginationParams, PaginationResult } from '@q-mono-x/types/base'
```

常用类型包括：

- `Primitive`
- `JsonValue`
- `QueryParams`
- `RequestBody`
- `Platform`
- `PaginationParams`
- `PaginationResult`

## 维护约定

新增声明时，请按职责放入对应目录：

| 场景                   | 放置位置  |
| ---------------------- | --------- |
| 跨端通用声明           | `env/`    |
| Web 专属增强           | `web/`    |
| Uni 专属增强           | `uni/`    |
| 显式引入的基础业务类型 | `base.ts` |

注意事项：

1. 不要把 Web 专属增强放入 `env/`，否则 Uni 项目会被污染。
2. 不要把 Uni 专属增强放入 `env/`，否则 Web/Node 包会被污染。
3. 如果只需要某个增强，优先使用 `@q-mono-x/types/<platform>/<module>` 精确引入。
4. `@q-mono-x/types` 和 `@q-mono-x/types/env` 应始终只代表跨端通用声明。
