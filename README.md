<!-- credits:start -->
> **Credits & license.** Port and maintenance by [JackStar6677-1](https://github.com/JackStar6677-1) and [DrakesCraft Labs](https://github.com/SlimefunNewHorizons). Original authors: JackStar (plugin.yml). Upstream license: **Other** (see [LICENSE](LICENSE)). Details: [docs/UPSTREAM_ATTRIBUTION.md](docs/UPSTREAM_ATTRIBUTION.md) · Policy: [Rights & Attribution](https://github.com/SlimefunNewHorizons/.github/blob/main/RIGHTS_AND_ATTRIBUTION.md). Independent community project, not affiliated with or endorsed by the original authors or the official Slimefun team.
>
> **Créditos y licencia.** Port y mantenimiento por [JackStar6677-1](https://github.com/JackStar6677-1) y [DrakesCraft Labs](https://github.com/SlimefunNewHorizons). Autores originales: JackStar (plugin.yml). Licencia original: **Other**. Detalle en [docs/UPSTREAM_ATTRIBUTION.md](docs/UPSTREAM_ATTRIBUTION.md).
<!-- credits:end -->

<p align="center">
  <img src="https://raw.githubusercontent.com/SlimefunNewHorizons/DrakesCrates/main/banner.svg" width="100%" alt="DRAKES CRATES animated banner" />
</p>

# DrakesCrates

> ### 🏰 Comunidad Oficial de DrakesCraft
>
> * 🎮 **IP del Servidor**: `mc.drakescraft.cl` *(Java & Bedrock)*
> * 💬 **Discord Oficial**: [discord.gg/rv3vtXZTk7](https://discord.gg/rv3vtXZTk7)
> * 🌐 **Portal Web & Guía**: [web.drakescraft.cl](https://web.drakescraft.cl)
> * 🛒 **Tienda Oficial**: [drakescraft-v2.tebex.io](https://drakescraft-v2.tebex.io)

---

Plugin de cajas de premios virtuales y físicas de DrakesCraft, con soporte para ítems de Slimefun, protección de modalidad **Clásico (Vanilla)**, ruleta visual animada y editor GUI de probabilidades sin editar YAML manualmente.

## Funcionalidades

- **Comando administrativo:** `/drakescrates givekey|editor|reload`.
- **Apertura de crates:** por bloque registrado en `crates.yml` con llaves físicas o virtuales.
- **Ruleta animada:** animación visual previa a la entrega del premio.
- **Editor GUI:** ajuste en vivo de `chance` por recompensa.
- **Preview pasivo:** clic izquierdo en el bloque de la crate para previsualizar recompensas y probabilidades.
- **Protección Clásico (Vanilla):** impide entregar ítems de Slimefun en mundos vanilla.
- **PlaceholderAPI:** `%drakescrates_keys_physical%` y `%drakescrates_keys_<key_id>%` (ej. `%drakescrates_keys_basic_key%`).
- **Recarga en caliente:** `/drakescrates reload` recarga `crates.yml` y `crates-settings.yml`.

## Arquitectura

- `application/`: casos de uso y repositorios.
- `domain/`: modelos `Crate`, `Reward`, `Key`, `OpenResult`.
- `infrastructure/`: parser YAML y settings.
- `presentation/`: comandos, listeners, editor GUI y animación.

## Configuración

- `src/main/resources/crates.yml`
- `src/main/resources/crates-settings.yml`

## Compatibilidad

| Componente | Requisito |
| :--- | :--- |
| **Servidor** | **Purpur 26.2** (Temporada 2) / Paper **1.21.x** |
| **Java** | Compilado con **JDK 21** (compatible con **Java 21 y Java 25**) |
| **Dependencias opcionales** | PlaceholderAPI, Votifier / VotifierPlus, Slimefun4-Drake |

## Compilación

Requisitos: **JDK 21+** y **Maven 3.9+**.

```bash
mvn clean package
```

---

## 📄 License & Intellectual Property

Copyright © 2026 [**JackStar6677-1**](https://github.com/JackStar6677-1) · [**DrakesCraft Labs**](https://github.com/SlimefunNewHorizons). All Rights Reserved.

This software is **Source-Available** for public inspection and technical audit. Redistribution, commercial repackaging, or unauthorized derivative distribution without explicit written permission from the author is strictly prohibited.
