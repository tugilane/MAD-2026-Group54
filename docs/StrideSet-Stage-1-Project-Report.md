# StrideSet – Sports Tracking App

## 1. Project Brief, App Idea and Business Goals

StrideSet is a lightweight Android fitness application combining gym workout logging, workout planning, progressive overload recommendations, and running tracking.

Existing fitness applications are often overloaded with advertisements, subscription requirements, social features, nutrition tools, or complicated interfaces. StrideSet targets users who want to record and review their training without these additional features.

The main product goals are to:

- Provide simple gym workout planning and logging.
- Combine gym and running activities in one application.
- Recommend progressive overload based on previous workouts.
- Keep the interface minimal and fast.
- Keep the course version free to operate using free-tier services.
- Build the app so it could later be released publicly if there is interest.

The project does not aim to provide nutrition or calorie tracking, advanced coaching, a social network, or a very large specialized exercise database. These would increase development scope without supporting the main goal of simple workout tracking.

## 2. Target Users and User Needs

The main target users are recreational athletes who want a simple way to track their training.

The main user needs are:

- Fast workout logging.
- Custom workout plans.
- Access to previous workout results.
- Flexible exercise and workout input.
- Simple progress recommendations.
- Basic running tracking.
- One application for both gym and running activities.
- Minimal clutter, advertisements, and unnecessary features.

### Personas

**Regular Gym User**

A university student or young adult who trains 3–5 times per week. They may currently record exercises, repetitions, and weights in a notes app or another fitness application. They want faster logging, saved workout plans, workout history, and simple progressive-overload recommendations.

**Mixed Training User**

A recreational athlete who combines gym training with running. They want one application instead of separate gym and running apps. They need workout history, gym planning, basic running statistics, and a simple interface.

## 3. Competitor Research

### Liftoff

Platform: Android and iOS.

Liftoff provides workout tracking, rankings, leaderboards, progression features, and social/gamification systems.

Strengths:
- Gamification makes training more engaging.
- Progress tracking and rankings provide motivation.
- Supports custom exercises and workout routines.

Weaknesses:
- The large number of social and gamification features can make the application more complicated than necessary for simple workout tracking.

What we can learn:
- Progress feedback is valuable, but StrideSet should provide it without building a large social or competitive system.

### MacroFactor Workouts

Platform: Android and iOS.

MacroFactor Workouts provides workout planning, detailed logging, progressive overload, exercise libraries, and automatically adjusted training programs.

Strengths:
- Polished and relatively simple interface.
- Strong progressive-overload functionality.
- Good workout-program organization.
- Large exercise library.

Weaknesses:
- Full use requires a paid subscription.
- It contains more advanced coaching functionality than StrideSet requires.

What we can learn:
- Progressive overload should be useful and transparent while remaining simple enough for users to understand and override.

### Arrow: Social Workout Tracker

Platform: iPhone.

Arrow combines workout logging with social feeds, leaderboards, achievements, and other gamification features.

Strengths:
- Minimal visual design.
- Social accountability and gamification.
- Progress and goal tracking.

Weaknesses:
- Social functionality adds complexity for users who only want personal training records.
- The original research also identified occasional reliability problems with displaying friends' workouts.

What we can learn:
- A clean interface is useful, but StrideSet should focus on the individual user's workouts rather than social interaction.

### Market Gap

The competitor research shows that strong workout applications already exist, but many combine tracking with subscriptions, social networks, gamification, advanced coaching, or other functionality.

StrideSet will focus on the gap between very basic manual tracking and feature-heavy fitness applications: a lightweight application that combines gym and running tracking with simple progression recommendations.

## 4. Feature Scope

### Core MVP Features

The first usable version requires:

- Gym workout logging: exercises, sets, repetitions, weights, and related information.
- Workout planning: users can create and save their own routines.
- Custom exercises.
- Workout history.
- Flexible manual workout input.
- Progressive-overload recommendations based on previous performance.
- Running tracking using Android location functionality and/or an external API.
- Local data persistence.

### Nice-to-Have Features

If development time remains, features will be implemented in this priority order:

1. Exercise and equipment library.
2. Favorite exercises for faster selection.
3. More detailed running statistics such as pace, speed, distance, duration, and step count.

Nice-to-have features will only be developed after the MVP is stable.

### Out of Scope

**Nutrition and calorie tracking:** unrelated to the core workout-tracking problem and would require additional data and UI development.

**Full social-network functionality:** would require accounts, feeds, moderation, and additional backend infrastructure.

**Complex fitness coaching:** requires significantly more training logic and validation than simple progression recommendations.

**Large specialized exercise libraries:** manually maintaining a large database would consume development time without being necessary for the MVP.

**Advanced AI training systems:** unnecessary for validating the main application concept and would substantially increase technical complexity.

### Riskiest MVP Feature

The riskiest feature is running tracking because it involves location permissions, background execution, GPS accuracy, battery usage, and possibly external APIs.

A small proof of concept will be created before full implementation. The running component will also be kept separate from the rest of the application. If the planned integration proves too difficult, it can be replaced with a simpler Android location-based implementation without affecting gym functionality.

## 5. Timeline and Effort

### Weeks 1–3: Planning

Define the product idea, target users, competitors, MVP scope, risks, assumptions, dependencies, and success metrics.

Milestone: Stage 1 Report – 17 September.

Estimated effort: 50–65 person-hours.

### Weeks 4–6: UX/UI Design

Create user journeys, navigation, wireframes, clickable prototypes, final screens, and developer-ready specifications.

Milestone: Stage 2 Report – 8 October.

Estimated effort: 50–65 person-hours.

### Weeks 7–8: Technical Validation and Midterm

Test technically risky areas such as running tracking, location permissions, local storage, Firebase, and API integration.

Milestone: Midterm Project Presentation.

### Weeks 9–12: Development

Implement the application structure, persistence, workout logging, workout planning, progressive overload, running functionality, and required integrations.

Milestone: Working MVP and Stage 3 Report – 19 November.

Estimated effort:
- Core Android development: 140–175 hours.
- Running/API integration: 50–65 hours.
- Progressive overload: 40–60 hours.

### Weeks 13–14: Testing

Perform functional, device, usability, persistence, regression, and crash testing. Fix identified problems.

Milestone: Stable peer-tested MVP.

### Weeks 15–16: Final Delivery

Prepare the release candidate, documentation, poster, demonstration, and presentation.

Milestone: Final poster session and project demonstration.

Testing, documentation, reporting, and final preparation are estimated at 75–90 hours.

Total expected project effort is approximately 450–500 person-hours, or around 90–100 hours per person for a five-person team.

The expected course-project cost is €0. Free services and free API tiers will be preferred. Public Google Play distribution is not required for the course and could introduce an additional developer-account cost later.

## 6. Risks

### Scope Creep

Likelihood: Medium  
Impact: High

Early warning sign: Nice-to-have features begin development before the MVP is complete.

Response: Freeze the MVP scope and remove optional functionality first if development falls behind.

### Running Tracking Becomes Too Complex

Likelihood: Medium  
Impact: High

Early warning sign: GPS, permission, background tracking, or API problems remain unresolved after the technical prototype.

Response: Isolate running functionality and replace the planned integration with simpler location tracking if necessary.

### Progressive Overload Produces Poor Recommendations

Likelihood: Medium  
Impact: Medium

Early warning sign: Test users repeatedly ignore or manually correct recommendations.

Response: Use a simple rules-based system. Recommendations remain optional and can always be overridden.

### Workout Data Is Lost or Incorrect

Likelihood: Low–Medium  
Impact: High

Early warning sign: Saved workouts fail persistence tests or produce inconsistent history.

Response: Use a stable local persistence system such as Room and test create, update, delete, save, and reload operations independently.

### Development Falls Behind Schedule

Likelihood: Medium  
Impact: High

Early warning sign: Core milestones are repeatedly moved or unfinished features accumulate.

Response: Prioritize MVP functionality and remove nice-to-have features.

### Git Conflicts or Duplicated Work

Likelihood: Medium  
Impact: Medium

Early warning sign: Multiple team members modify the same functionality without coordination.

Response: Assign GitHub issues, use feature branches, and merge through pull requests.

### External Service Changes

Likelihood: Low  
Impact: Medium

Early warning sign: API limits, authentication, pricing, or documentation change.

Response: Access external services through separate interfaces so they can be replaced without major changes to the rest of the application.

## 7. Assumptions

**The initial application will have a small user base.**  
If this is false, the architecture may require more scalable backend infrastructure. The MVP will still prioritize local storage.

**Most gym functionality can work locally.**  
If this is false, additional backend functionality will be required. Core workout logging should remain offline-capable wherever possible.

**Progressive overload can use a simple rules-based system.**  
If testing shows this is insufficient, the feature will be reduced to basic suggestions rather than expanded into a complex coaching system.

**Team members will have enough time to complete their assigned work.**  
If availability decreases, optional features will be removed and development will focus only on the MVP.

## 8. External Dependencies

**Android SDK and Kotlin/course technologies:** required for application development. If a particular library causes problems, standard Android alternatives will be preferred.

**Room or another persistence solution:** required for reliable workout history. If Room is unsuitable, another supported local database can replace it.

**Android location services/external running API:** required for running tracking. If the external API is unsuitable, direct Android location tracking will be used.

**Firebase or equivalent backend:** only required if authentication or synchronization is implemented. If backend integration becomes too complex, the MVP can remain primarily local.

**GitHub:** used for source control, issues, pull requests, and project coordination. Local copies of the repository provide protection against temporary service availability problems.

## 9. Success Metrics

The project will be considered successful if the final MVP achieves the following during testing:

- At least 90% of test users can create a workout plan, log a workout, view a progression recommendation, and track a run without assistance.
- At least 99% of testing sessions complete without an application crash.
- Normal local actions respond within approximately 500 ms.
- Average usability feedback reaches at least 4/5.
- At least 95% of workout and running records save, reload, and display correctly during structured testing.

## 10. GitHub and Team Process

The GitHub repository will contain the application source code, project documentation, and a README explaining the project, setup process, and main functionality.

Development work will be divided into GitHub issues. Each issue will have an assigned owner. Features will be developed on separate branches and merged into the main branch through pull requests. Significant changes should be reviewed before merging.

The team will communicate through a shared team chat, GitHub, and shared project documents. At least one project meeting will be held each week to review progress, distribute tasks, identify blockers, and make scope decisions.

Major project decisions should be agreed by the relevant team leads and documented in project materials or GitHub when they affect implementation.

## 11. AI-Assisted Work

Generative LLM tools were used to convert team-written notes and bullet points into clearer report text and to help reorganize the document.

The app concept, target users, feature scope, competitor selection, priorities, responsibilities, technical decisions, and final project decisions were made by the team.

AI-generated text was reviewed against the team's original notes and edited before inclusion. Factual information used for competitor platforms was checked against current App Store, Google Play, or official product information. AI-generated content was not accepted as a project decision without team review.