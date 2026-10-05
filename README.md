<p align="center">
  <img src="https://raw.githubusercontent.com/SlimefunNewHorizons/DrakesCrates/master/banner.svg" width="100%" alt="DRAKES CRATES animated banner" />
</p>

# DrakesCrates

> ### 🏰 ¡Únete a la Comunidad Oficial de DrakesCraft!
> 
> * 🎮 **IP del Servidor**: `mc.drakescraft.cl` *(Java 1.21.11 & Bedrock)*
> * 💬 **Discord Oficial**: [discord.gg/drakescraft](https://discord.gg/rv3vtXZTk7)
> * 🌐 **Web & Guía**: [web.drakescraft.cl](https://web.drakescraft.cl) — 🛒 **Tienda**: [web.drakescraft.cl/store](https://web.drakescraft.cl/store.html)
> 
> *¡Juega con este addon y más de 80 expansiones optimizadas en vivo en nuestra network de supervivencia técnica!*

---

Plugin de crates extraido desde el modulo `drakescrates` del antiguo `DrakesCore`.

## Objetivo
Gestionar cajas de premios con llaves fisicas, ruleta visual y edicion de probabilidades sin tocar YAML manualmente.

## Que hace hoy
- Comando admin: `/drakescrates givekey|editor|reload`.
- Apertura de crates por bloque registrado en `crates.yml`.
- Animacion tipo ruleta antes de entregar premio.
- Editor GUI para ajustar `chance` por reward.
- Preview pasivo al click izquierdo en el bloque de crate.
- PlaceholderAPI: `%drakescrates_keys_physical%`.
- PlaceholderAPI por llave: `%drakescrates_keys_<key_id>%` (ej: `%drakescrates_keys_basic_key%`).
- Recarga runtime de `crates.yml` y `crates-settings.yml` con `/drakescrates reload`.

## Architecture heredada del Core
- `application/`: casos de uso y repositorio.
- `domain/`: `Crate`, `Reward`, `Key`, `OpenResult`.
- `infrastructure/`: parser YAML y settings.
- `presentation/`: comandos, listeners, editor, animacion.

## Configuracion
- `src/main/resources/crates.yml`
- `src/main/resources/crates-settings.yml`

## Dependencias
- Paper 1.20.6
- Java 21
- PlaceholderAPI (opcional)

## Pendiente real
- Reportes/export de configuracion para auditoria de economia.

---

## 📄 License & Intellectual Property

Copyright © 2026 [**JackStar6677-1**](https://github.com/JackStar6677-1) · [**DrakesCraft Labs**](https://github.com/SlimefunNewHorizons). All Rights Reserved.

This software is **Source-Available** for public inspection and technical audit. Redistribution, commercial repackaging, or unauthorized derivative distribution without explicit written permission from the author is strictly prohibited.
