# Estrategia de Branching — CircleGuard

## Metodología: GitHub Flow

Elegimos GitHub Flow sobre GitFlow por su simplicidad para equipos pequeños
y timelines cortos. No hay ramas develop, release ni hotfix — solo master y
ramas de trabajo cortas.

## Diagrama

```
master ──●──────────────────────●────────────────────────●──── (protegida)
          │                      │                         │
feat/...──┘   fix/...────────────┘   chore/...────────────┘
              (PR + CI verde + 1 review)
```

## Reglas

### Rama master
- Siempre desplegable — representa el estado productivo del proyecto
- Protegida: no acepta push directo
- Solo acepta merges vía Pull Request con:
  - CI verde (build + tests + SonarCloud + Trivy)
  - Mínimo 1 review aprobado del otro integrante

### Ramas de trabajo
Formato: `<tipo>/<numero-issue>-descripcion-corta`

| Tipo | Cuándo usarlo | Ejemplo |
|---|---|---|
| feat | Nueva funcionalidad | feat/2-terraform-modular |
| fix | Corrección de bug | fix/sprint0-bugs |
| chore | Configuración, setup, herramientas | chore/branching-strategy-doc |
| docs | Solo documentación | docs/architecture-diagram |

### Ciclo de vida de una rama
1. `git checkout -b feat/<N>-descripcion`
2. Desarrollar + commits con formato convencional
3. `git push origin feat/<N>-descripcion`
4. Abrir Pull Request → asignar al otro integrante para review
5. CI pasa → review aprobado → merge a master
6. Rama eliminada después del merge

## Formato de commits (Conventional Commits)

Requerido para que semantic-release genere tags automáticamente:

| Prefijo | Efecto en versión | Ejemplo |
|---|---|---|
| feat: | bump minor → v1.**1**.0 | feat: agregar circuit breaker en auth-service |
| fix: | bump patch → v1.0.**1** | fix: corregir puerto identity-service en dev |
| chore: | sin bump | chore: actualizar dependencias |
| docs: | sin bump | docs: agregar diagrama de arquitectura |
| BREAKING CHANGE | bump major → **2**.0.0 | en el body del commit |

## Ambientes y promoción

| Ambiente | Namespace k8s | Trigger | Aprobación |
|---|---|---|---|
| dev | circleguard-dev | Automático en merge a master | No requerida |
| stage | circleguard-stage | Manual desde GHA | Requerida (1 persona) |
| prod | circleguard-prod | Manual desde GHA | Requerida (1 persona) |

## Release y etiquetado

Los tags de versión (`v1.0.0`, `v1.1.0`) los genera **semantic-release**
automáticamente en cada merge a master, basándose en los tipos de commits.
El `CHANGELOG.md` se actualiza automáticamente con **git-cliff**.

## Herramientas
- Gestión de tareas: GitHub Projects (Kanban)
- Issues como user stories con criterios de aceptación
- Labels por sprint y área: sprint-1, terraform, ci-cd, observabilidad, etc.
