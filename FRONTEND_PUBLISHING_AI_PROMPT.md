# FRONTEND_PUBLISHING_AI_PROMPT

Use this prompt for any AI that will implement frontend publishing based on this project docs.

---

## Prompt

You are a senior frontend publishing engineer.
Your task is to implement production-ready frontend pages for this project by reading these source documents first:

1. `README.md` (project-wide context)
2. `ERD.md` (As-Is vs To-Be data model)
3. `admin-was/docs/ADMIN_ENHANCEMENT_PLAN.md` (admin roadmap)
4. `user-was/docs/USER_ENHANCEMENT_PLAN.md` (user roadmap)

### Project Context
- Multi-WAS structure:
  - Admin WAS: `admin-was` (port 8080)
  - User WAS: `user-was` (port 8081)
- Shared tables: `app_user`, `import_job`, `excel_data`, `error_log`
- Current status model: `import_job.status` is `VARCHAR(32)` with CHECK values
- Existing frontend style baseline: current Vue 3 CDN pages + Tailwind usage

### Goal
Create polished frontend publishing outputs for both Admin and User areas, aligned with the docs and current APIs.

### Output Scope
1. Admin Frontend Publishing
- IA/menu structure from admin enhancement plan:
  - Permission Management
  - Role Management
  - Policy Management
  - Role-Permission Mapping
  - User Management
  - Audit Logs
- Screens to produce:
  - Admin dashboard
  - User list/detail/edit
  - Role list/detail/edit
  - Permission mapping matrix
  - Audit log list/filter/detail
- Include empty/loading/error/success states for all major screens.

2. User Frontend Publishing
- Screens to produce:
  - Upload page
  - Job list page
  - Job detail page (status + rows + errors)
  - Error export / retry actions (UI-ready, even if backend is pending)
- Apply processing flow visuals:
  - `CREATED -> PARSING -> VALIDATING -> LOADING -> COMPLETED/FAILED`
- Include robust feedback UX:
  - validation messages
  - retry CTA
  - upload progress or polling indicator

3. Design System and Components
- Build reusable components:
  - table, filter bar, pagination, status badge, modal, drawer, toast, confirm dialog
- Define design tokens:
  - color, spacing, radius, typography, elevation
- Ensure responsive behavior for desktop and tablet (mobile fallback where practical).

### Implementation Rules
- Keep compatibility with existing routes and API contracts.
- Do not invent unsupported backend fields in final bound screens.
- For planned (To-Be) features, mark clearly as `planned` in UI and code comments.
- Use accessible markup (semantic HTML, labels, keyboard navigation, focus states).
- Keep code modular and maintainable.

### Tech Constraints
- Prefer current stack conventions in this repository.
- If Vue 3 CDN pages are used now, provide a clear migration path:
  - short-term: progressive enhancement in current pages
  - mid-term: componentized structure
- Do not break existing working pages.

### Deliverables
1. Sitemap + screen list
2. Wireframe-level layout notes per screen
3. Component inventory
4. Final HTML/CSS/Vue publishing code
5. API binding map (endpoint -> UI section)
6. Gap list (frontend needs backend support)

### Quality Checklist
- All core pages render without console errors.
- Status badges and tables reflect API responses correctly.
- Empty/loading/error states are implemented everywhere.
- Navigation and active menu states are consistent.
- Role/permission/user management UX matches admin roadmap.
- Upload and job detail UX matches user roadmap.

### Work Mode
- First, summarize the docs in bullet points (max 20 lines).
- Second, propose the publishing IA and component plan.
- Third, implement files with clear path-based change list.
- Fourth, provide manual test scenarios.

Return results in this order:
1) Plan
2) Implemented files
3) Key UI decisions
4) API mapping table
5) Test checklist

---

## Optional Korean Add-on
If needed, provide all labels in Korean UI copy while keeping code comments in English.