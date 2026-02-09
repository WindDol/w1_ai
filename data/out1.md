

Chaos
An Interdisciplinary Journal of Nonlinear Science

RESEARCH ARTICLE | SEPTEMBER 14 2021

# The Kuramoto model on a sphere: Explaining its low-dimensional dynamics with group theory and hyperbolic geometry

Max Lipton; Renato Mirollo; Steven H. Strogatz

Check for updates

Chaos 31, 093113 (2021)
https://doi.org/10.1063/5.0060233
CHORUS

View Online | Export Citation

## Articles You May Be Interested In

Ott–Antonsen ansatz for the D-dimensional Kuramoto model: A constructive approach

Chaos (November 2021)

The asymptotic behavior of the order parameter for the infinite-N Kuramoto model

Chaos (November 2012)

Synchronization of relativistic particles in the hyperbolic Kuramoto model

Chaos (May 2018)

AIP Publishing

Chaos

Special Topics Open for Submissions

Learn More

21 October 2024 09:50:11






Chaos    ARTICLE    scitation.org/journal/cha

# The Kuramoto model on a sphere: Explaining its low-dimensional dynamics with group theory and hyperbolic geometry

**Cite as:** Chaos **31**, 093113 (2021); doi: 10.1063/5.0060233  
**Submitted:** 16 June 2021 · **Accepted:** 23 August 2021 ·  
**Published Online:** 14 September 2021

Max Lipton,<sup>1,a)</sup> Renato Mirollo,<sup>2,b)</sup> and Steven H. Strogatz<sup>1,c)</sup>

## AFFILIATIONS

<sup>1</sup>Department of Mathematics, Cornell University, Ithaca, New York 14853, USA  
<sup>2</sup>Department of Mathematics, Boston College, Chestnut Hill, Massachusetts 02467, USA

<sup>a)</sup>**Electronic mail:** ml2437@cornell.edu  
<sup>b)</sup>**Electronic mail:** mirollo@bc.edu  
<sup>c)</sup>**Author to whom correspondence should be addressed:** shs7@cornell.edu

## ABSTRACT

We study a system of N identical interacting particles moving on the unit sphere in d-dimensional space. The particles are self-propelled and coupled all to all, and their motion is heavily overdamped. For d = 2, the system reduces to the classic Kuramoto model of coupled oscillators; for d = 3, it has been proposed to describe the orientation dynamics of swarms of drones or other entities moving about in three-dimensional space. Here, we use group theory to explain the recent discovery that the model shows low-dimensional dynamics for all N ≥ 3 and to clarify why it admits the analog of the Ott–Antonsen ansatz in the continuum limit N → ∞. The underlying reason is that the system is intimately connected to the natural hyperbolic geometry on the unit ball B<sup>d</sup>. In this geometry, the isometries form a Lie group consisting of higher-dimensional generalizations of the Möbius transformations used in complex analysis. Once these connections are realized, the reduced dynamics and the generalized Ott–Antonsen ansatz follow immediately. This framework also reveals the seamless connection between the finite and infinite-N cases. Finally, we show that special forms of coupling yield gradient dynamics with respect to the hyperbolic metric and use that fact to obtain global stability results about convergence to the synchronized state.

Published under an exclusive license by AIP Publishing. https://doi.org/10.1063/5.0060233

Exactly solvable models have long played a central role in nonlinear dynamics, from Newton's work on the gravitational two-body problem to breakthroughs in understanding solitons in the 1970s. Often, the solvability of a model reflects an underlying symmetry or other special structure in its governing equations. In this paper, we discuss a many-body system of current interest, known as the Kuramoto model on a sphere, whose unexpectedly low-dimensional dynamics call out for explanation. The model consists of N identical overdamped particles moving on a (d − 1)-dimensional sphere in d-dimensional Euclidean space, yielding a state space of dimension N(d − 1). Yet, despite the presence of damping, the model exhibits enormously many constants of motion. Here, we show that its trajectories are confined to invariant manifolds of dimension d(d + 1)/2 for all N ≥ 3 and trace the origin of this low-dimensional behavior to an underlying group-theoretic structure in the system. Specifically, the Kuramoto model on a sphere turns out to be the flow induced by the action of the group of Möbius transformations on the d-dimensional ball, and its invariant manifolds are the associated group orbits. For certain forms of coupling, the model acquires a further structure (hyperbolic gradient dynamics) that forces almost all solutions to converge to the perfectly synchronized state.

## I. INTRODUCTION

In 1975, Kuramoto introduced a model for a large population of coupled oscillators with randomly distributed natural frequencies.<sup>1</sup> Kuramoto's model displayed many remarkable features: It was exactly solvable (at least in some sense) despite being nonlinear and infinite-dimensional.<sup>2</sup> Its solution shed analytical light on a phase transition to mutual synchronization that Winfree had previously discovered in a similar but less convenient system of oscillators.<sup>3,4</sup> Since then, the Kuramoto model has been an object of

Chaos 31, 093113 (2021); doi: 10.1063/5.0060233       31, 093113-1  
Published under an exclusive license by AIP Publishing






Chaos    ARTICLE  scitation.org/journal/cha

fascination for nonlinear dynamicists, as well as a simplified model for many real-world instances of coupled oscillators in physics, biology, chemistry, and engineering.<sup>5–12</sup>

From a mathematical standpoint, one of the most intriguing problems has been to explain the tractability of the Kuramoto model. What symmetry or other hidden structure accounts for its solvability?

The first clues came from work on an adjacent topic: series arrays of N identical overdamped Josephson junctions. The governing equations for those superconducting oscillators are closely related to the equations of the Kuramoto model<sup>13,14</sup> and themselves displayed remarkable dynamical features, such as surprisingly low-dimensional invariant tori<sup>15,16</sup> and ubiquitous neutral stability of splay states<sup>17</sup> despite the presence of damping and driving in the governing equations. These features were explained in 1993 by the discovery of a certain change of variables, now called the Watanabe–Strogatz transformation,<sup>18,19</sup> which showed that the governing equations have N − 3 constants of motion for all N ≥ 3. Goebel<sup>20</sup> then pointed out that the Watanabe–Strogatz transformation could be viewed as a time-dependent version of a linear fractional transformation, a standard tool in complex analysis. For more than a decade, however, these results did not attract much attention perhaps because they were assumed to be restricted to problems about Josephson junctions and within that specialized setting, they were even further restricted to junctions that were strictly identical.

A breakthrough occurred in 2008 with the work of Ott and Antonsen.<sup>21,22</sup> They found an astonishing way to capture the macroscopic dynamics of the infinite-N Kuramoto model even when the oscillators' frequencies were non-identical and randomly distributed. First, they wrote down an ansatz—seemingly pulled out of thin air—for the density function ρ(θ, ω, t) of oscillators having phase θ and intrinsic frequency ω at time t. Their ansatz had the form of a time-dependent Poisson density (a density better known for its role in the study of partial differential equations, specifically for the solution of Laplace's equation on a disk, given the values of the unknown function on the bounding circle). By making this ansatz of a Poisson density, Ott and Antonsen reduced the infinite-N Kuramoto model, an integro-partial differential equation, to an infinite set of coupled ordinary differential equations. Then, by further assuming that the intrinsic frequencies of the oscillators were randomly distributed according to a Lorentzian (Cauchy) distribution, Ott and Antonsen showed that the order parameter dynamics of the Kuramoto model could be reduced tremendously, all the way down to an ordinary differential equation for a single scalar variable, the amplitude of the order parameter.<sup>21</sup> With this discovery, the floodgates were now open. Almost immediately, the Ott–Antonsen ansatz was used to solve many longstanding problems about the Kuramoto model and its variants, as well as to generate and solve many new problems.<sup>10</sup>

Still, a lot of old questions hung in the air. Both the Watanabe–Strogatz transformation and the Ott–Antonsen ansatz appeared somewhat unmotivated and almost miraculous. Where did they come from, and why did they work? It was also not clear whether they were connected or perhaps even equivalent. There were reasons to doubt that they were linked: the Watanabe–Strogatz transformation could be used for any finite N ≥ 3 but seemed restricted to identical oscillators, whereas the Ott–Antonsen ansatz allowed for non-identical oscillators but seemed restricted to the continuum limit of infinite N. Also, why were linear fractional transformations and Poisson densities—tools from other branches of mathematics—popping up in these studies of dynamical systems?

Later work made sense of all of this. The Josephson arrays and the Kuramoto model both turned out to have deep mathematical ties to group theory, hyperbolic geometry, and projective geometry, and both the Watanabe–Strogatz transformation and the Ott–Antonsen ansatz were tapping into these structures.<sup>10,23–27</sup> For the Josephson arrays, the governing equations turned out to be generated by a group action, specifically the action of the Möbius group of linear fractional transformations of the unit disk to itself. Seen in this light, the constants of motion for the Josephson arrays were cross-ratios, and the invariant tori were group orbits. The same group-theoretic structure was found to underlie the Kuramoto model (in the special case where all the oscillator frequencies are identical) as well as other sinusoidally coupled systems of identical phase oscillators.<sup>24,26</sup>

In the past few years, several researchers wondered how far this story could be pushed. Are there quantum or higher-dimensional extensions of the Kuramoto model that might show similar reducibility? A number of results along these lines have now been found.<sup>28–45</sup> In particular, several researchers have explored a generalization of the Kuramoto model in which the oscillators move on spheres instead of the unit circle; the spheres could be either the ordinary two-dimensional sphere or higher-dimensional spheres. In particular, a system of particles moving on the two-sphere has been used to model the orientation dynamics of swarms of drones flying around in three dimensions.<sup>46</sup> As we shall demonstrate, these higher-dimensional oscillator models exhibit a dynamical reduction generalizing the reduction for the Kuramoto model. Consequently, the computational effort required to simulate these systems can be substantially reduced. A counterpart of the Ott–Antonsen ansatz has also been discovered for the continuum version of the Kuramoto model for identical oscillators on the d-dimensional sphere and used to reduce its infinite-dimensional dynamics to a lower dimensional set of ordinary differential equations (ODEs).<sup>38</sup> However, as before, some of the results appear disconnected and a bit miraculous.

Our goal in this paper is to show that hyperbolic geometry and group theory can unify and clarify our understanding of the Kuramoto model on a sphere and make all the latest results seem natural, just as they did before for the traditional Kuramoto model. We focus exclusively on the case of identical oscillators, though we expect that our methods will extend to systems with multiple populations of identical oscillators or a continuum of oscillator families with a distribution of natural frequencies. Our approach explains the model's reducibility for any finite number of oscillators, as well as for the continuum limit, and it reveals why Poisson densities arise again in this setting. There is a close connection to Laplace's equation and harmonic analysis, as we will see in Sec. V. We also find that complex analysis is not really essential, which is just as well, since it does not generalize to the higher-dimensional spheres being considered here. On the contrary, the proper mathematical setting is harmonic analysis and hyperbolic geometry on higher-dimensional balls. Our work also allows us to go beyond merely unifying existing results. For instance, by establishing that linearly coupled systems of identical Kuramoto oscillators on a sphere have a hyperbolic gradient structure, we can prove new global stability

Chaos 31, 093113 (2021); doi: 10.1063/5.0060233       31, 093113-2
Published under an exclusive license by AIP Publishing






Chaos    ARTICLE  scitation.org/journal/cha

results about convergence to the synchronized state, as described in Sec. VIII.

## II. PRELIMINARIES

### A. The Kuramoto model on a sphere

In a pioneering paper, Lohe<sup>28</sup> observed that there are at least two natural generalizations of the Kuramoto model to higher dimensions. One of them replaces the phases $\theta_j$ of the original Kuramoto model<sup>1,2</sup> with complex numbers $\exp(i\theta_j)$ on the unit circle and then views those as equivalent to $2 \times 2$ rotation matrices parameterized by a rotation angle $\theta_j$. From there, it is a natural step to consider other Lie groups of matrices, many of which are non-Abelian.

Our concern in this paper, however, is with a different generalization of the Kuramoto model. Instead of regarding oscillators as particles moving on the unit circle, we think of them as particles moving on the unit sphere. The sphere could be the surface of the ordinary unit ball in three dimensions or some higher-dimensional sphere $S^{d-1}$ in $\mathbb{R}^d$. When $d = 2$, the sphere reduces to the unit circle in the plane, and the model reduces to the original Kuramoto model.

The governing equations for the *Kuramoto model on a sphere* are

$$\dot{x}_i = A_i x_i + Z - \langle Z, x_i \rangle x_i, \quad i = 1, \ldots, N,$$ (1)

where $x_i$ is a point on the unit sphere $S^{d-1} \subset \mathbb{R}^d$, each $A_i$ is an antisymmetric $d \times d$ matrix, and $Z \in \mathbb{R}^d$ is a $d$-dimensional vector analogous to the complex order parameter for the classic Kuramoto model. In Eq. (1), the matrix $A_i$ and the vector $Z$ are functions of the configuration $(x_1, \ldots, x_N)$ of points on the sphere. Note that $Z$ does not depend on $i$; like the usual Kuramoto order parameter, it plays the role of a mean-field quantity that couples all the "oscillators" $x_i$ together. The antisymmetric matrix $A_i$ is the higher-dimensional counterpart of an intrinsic frequency $\omega_i$ in the original Kuramoto model.

A straightforward computation shows that the dot product between an oscillator's instantaneous position and instantaneous velocity satisfies $\langle x_i, \dot{x}_i \rangle = 0$, which proves that oscillators that start on the unit sphere stay on it forever. The state space for this system is the $N$-fold product $X = (S^{d-1})^N$, which has dimension $N(d - 1)$. Later, we will also consider the natural infinite-$N$ analog of (1), where a state is a probability measure on $S^{d-1}$.

In what follows, we allow $Z$ to be any smooth function on the state space $X$, though in examples, we usually restrict to fairly simple functions, such as a linear combination of the form

$$Z = \sum_{i=1}^N a_i x_i,$$

where the $a_i$ are real constants.

### B. General philosophy: Lie groups and reducible systems

There is a general technique for dimensional reduction of systems such as (1) that we pause to describe. Suppose we have a smooth manifold $X$, which we think of as a state space, and a group $G$ acting on $X$, where $G$ is also a smooth manifold (in other words, $G$ is a Lie group). Then, the group action induces a space of vector fields on $X$, the so-called infinitesimal generators of the action.

To construct these generators, let $\gamma(t)$ be a smooth curve in $G$ with $\gamma(0) = e$, the identity element in $G$. Then, the derivative $\dot{\gamma}(0) = v$, where $v$ is a vector in the tangent space $T_e G$ of $G$ at $e$. This vector $v$ is in turn associated very naturally with a corresponding vector $\tilde{v}$ in the tangent space of $X$, as follows. For each $x \in X$, $t \mapsto \gamma(t)x$ is a smooth curve in $X$, and its derivative at $t = 0$ defines a vector $\tilde{v}_x$ in the tangent space at $x$. The vector field $\tilde{v} = (\tilde{v}_x)$ is the infinitesimal generator corresponding to the element $v$ in the tangent space of $G$ at $e$. At each point $x \in X$, the infinitesimal generators $\tilde{v}_x$ span a linear subspace $V_x$ of the tangent space $T_x X$, which is exactly the set of vectors tangent to the group orbit $Gx$ at the element $x$.

Now, suppose we have a vector field $\xi$ on $X$, which defines a dynamical system on the state space $X$. If $\xi_x \in V_x$ at each point $x \in X$, then the flow corresponding to the vector field $\xi$ will be constrained to lie on the group orbits $Gx$. If the dimension of $G$ is less than that of the state space $X$, then this will give us a dimensional reduction of the dynamics from dim $X$ to dim $G$. Now, suppose, as is the case in applications of this methodology, the correspondence $G \to Gx$ is one-to-one for generic $x \in X$; equivalently, the stabilizer subgroup $G_x = \{e\}$ for generic $x \in X$. Then, for each $x \in X$, the flow on the group orbit $Gx$ is equivalent to a flow on $G$, which is a lower dimensional space than the state space $X$.

Here is a familiar example: Consider the orthogonal group $G = SO(d)$ consisting of orientation-preserving linear isometries of $\mathbb{R}^d$. (For an intuitive picture, think of these isometries as rotations.) Then, $G$ acts on $\mathbb{R}^d$, and the corresponding infinitesimal generators are the linear vector fields $\tilde{v}_x = Ax$, where $A$ is any skew-symmetric matrix. We can also let $G$ act on the product space $X = (\mathbb{R}^d)^N$ of $N$-tuples $x = (x_i)$, $x_i \in \mathbb{R}^d$, and then the infinitesimal generators have the form $(\tilde{v}_x)_i = Ax_i$ for some skew-symmetric matrix $A$. We could also, if we like, restrict $X$ to $N$-tuples $(x_i)$ with $x_i \in S^{d-1}$, the state space for (1). Now, suppose we had a dynamical system on $X$ of the form $\dot{x}_i = Ax_i$, where the skew-symmetric matrix $A$ is a function of the configuration $x = (x_i)$; this is just the special case of (1) with $Z = 0$. Then, the dynamics on $X$ reduces to dynamics on $G$, which has dimension $d(d - 1)/2$, and for large $N$, this is much smaller than the dimension of $X$, which is $N(d - 1)$. Basically, the configuration $(x_i)$ of points on the sphere $S^{d-1}$ can only move collectively by a rotation of the sphere; therefore, the dynamics reduces to a dynamical system on $SO(d)$.

We want to apply this methodology to the system (1). However, since that system generally has $Z \neq 0$, we need a different group action to make this strategy work. Fortunately, vector fields of the form seen in the Kuramoto model,

$$\dot{x} = Ax + Z - \langle Z, x \rangle x,$$ (2)

turn out to arise as the infinitesimal generators of the group action of a larger group $G$ acting on the sphere $S^{d-1}$ and its interior, the unit ball $B^d$. This larger group is the Möbius group of isometries of the hyperbolic geometry on $B^d$. It contains the orthogonal group as a proper subgroup but has bigger dimension $d(d + 1)/2$.

Chaos 31, 093113 (2021); doi: 10.1063/5.0060233       31, 093113-3
Published under an exclusive license by AIP Publishing






Chaos    ARTICLE  scitation.org/journal/cha

Therefore, for the Kuramoto system (1), if the matrices $A_i$ all happen to be identical, we can reduce the dynamics of the system to a much smaller system on this Möbius group $G$ and use this reduction to understand the dynamics on the larger state space $X$. This is the essence of the approach we take. Ultimately, we apply it to prove a synchronization theorem for the system (1) for the special order parameter $Z = \sum_{i=1}^N a_i x_i$ with $a_i > 0$. However, first, we need to show that vector fields on $S^{d-1}$ of the form in (2) are indeed the infinitesimal generators of the action of the larger Möbius group.

## C. Hyperbolic geometry and Möbius transformations

In this paper, a *Möbius transformation* is a composition of Euclidean isometries and spherical inversions of $\mathbb{R}^d$ mapping the unit ball homeomorphically to itself and preserving orientation. This is a more restrictive definition than the commonly defined Möbius transformations, which, in general, do not need to preserve the unit ball.

As in the case $d = 2$, flows of the form (1) are intimately related to the natural hyperbolic geometry on the unit ball $B^d$ with boundary $S^{d-1}$. This geometry has metric

$$ds = \frac{2|dx|}{1 - |x|^2},$$

where $|dx|$ is the ordinary Euclidean metric. Isometries are assumed to be with respect to this hyperbolic geometry unless otherwise qualified as Euclidean. The metric $ds$ has a constant (sectional) curvature equal to $-1$, and we can describe its isometries, which generalize the Möbius transformations preserving the unit disk for $d = 2$. For $d = 2$, let $w \in B^2$ and consider the Möbius transformation

$$M_w(x) = \frac{x - w}{1 - \overline{w}x},$$

which preserves the unit disk $B^2$ and its boundary $S^1$. To generalize this to higher dimensions, we need to express $M_w(x)$ without reference to complex arithmetic operations or conjugation. This is the goal of Subsection II C 1.

### 1. *Möbius transformations in higher dimensions*

Using the identity $2\langle w, x\rangle = \overline{w}x + w\overline{x}$, we see that

$$\frac{(x - w)(1 - w\overline{x})}{(1 - \overline{w}x)(1 - w\overline{x})} = \frac{x - w - w|x|^2 + w^2\overline{x}}{1 - 2\langle w, x\rangle + |w|^2|x|^2}$$

$$= \frac{x - w - w|x|^2 + w(2\langle w, x\rangle - \overline{w}x)}{1 - 2\langle w, x\rangle + |w|^2|x|^2}$$

$$= \frac{(1 - |w|^2)x - (1 - 2\langle w, x\rangle + |x|^2)w}{1 - 2\langle w, x\rangle + |w|^2|x|^2}.$$

This form of $M_w$ generalizes to higher dimensions: Let $w \in B^d$ and define

$$M_w(x) = \frac{(1 - |w|^2)x - (1 - 2\langle w, x\rangle + |x|^2)w}{1 - 2\langle w, x\rangle + |w|^2|x|^2}$$

$$= \frac{(1 - |w|^2)(x - |x|^2w)}{1 - 2\langle w, x\rangle + |w|^2|x|^2} - w,$$

where $x \in B^d$ or $S^{d-1}$. We call $M_w$ a *boost transformation*.

If $|x| = 1$, this formula simplifies to

$$M_w(x) = \frac{(1 - |w|^2)(x - w)}{|x - w|^2} - w.$$

Now, see that

$$M_w(w) = \frac{(1 - |w|^2)w - (1 - 2\langle w, w\rangle + |w|^2)w}{1 - 2\langle w, w\rangle + |w|^2|w|^2}$$

$$= \frac{(1 - |w|^2)w - (1 - |w|^2)w}{1 - 2\langle w, w\rangle + |w|^2|w|^2} = 0.$$

Alternatively, we can use the second form to show

$$M_w(w) = \frac{(1 - |w|^2)(w - |w|^2w)}{1 - 2\langle w, w\rangle + |w|^2|w|^2} - w = \frac{(1 - |w|^2)^2w}{(1 - |w|^2)^2} - w = 0.$$

Similar computations show that $M_0$ is the identity, $M_w^{-1} = M_{-w}$, and $M_w(0) = -w$.

It is a standard result in hyperbolic geometry (e.g., see Theorem 3.5.1 in Beardon$^{47}$) that any orientation-preserving isometry of $B^d$ can be expressed uniquely as the product of a boost and a rotation (an orientation-preserving orthogonal transformation), and these operations can be done in either order. In other words, any such isometry can be written uniquely in the form

$$g(x) = \zeta M_w(x)$$

and also uniquely in the form

$$g(x) = M_{-z}(\xi x)$$

for some vectors $w, z \in B^d$ and rotations $\zeta, \xi \in SO(d)$, where $SO(d)$ denotes the group of orientation-preserving orthogonal linear transformations on $\mathbb{R}^d$. Therefore, counting the extra $d$ dimensions that we get from the vector $w$ or $z$, we see that the Möbius group has dimension $d + d(d - 1)/2 = d(d + 1)/2$.

Depending on the situation, one of these two forms might be more useful than the other, even though they are equivalent. In the interest of flexibility, it is useful to find how the parameter pairs $w, \zeta$ and $z, \xi$ are related. We can do this by comparing the linearizations of the two formulas for $g(x)$ at $x = 0$,

$$g(x) \approx \zeta(-w + (1 - |w|^2)x) \approx z + (1 - |z|^2)\xi x.$$

Equating coefficients implies $z = -\zeta w$ (hence, $|z| = |w|$) and $\xi = \zeta$.

### 2. *Infinitesimal generators*

Having parameterized the Möbius transformations, we are now ready to derive the associated infinitesimal generators of the Möbius group action on the ball $B^d$. We will show that they correspond to flows of the form

$$\dot{y} = Ay - \langle Z, y\rangle y + \frac{1}{2}(1 + |y|^2)Z, \tag{3}$$

where $A$ is an antisymmetric $d \times d$ matrix and $Z \in \mathbb{R}^d$ is a vector. Note that, as advertised, this flow extends to a flow on $S^{d-1}$ of the Kuramoto form in (2), as we can see by restricting (3) to vectors $y$ on the unit sphere where $|y| = 1$.

Chaos 31, 093113 (2021); doi: 10.1063/5.0060233       31, 093113-4
Published under an exclusive license by AIP Publishing






Chaos    ARTICLE  scitation.org/journal/cha

To derive (3), we work separately with the boost and rotation components. Let us start with the boost component. Replace $w$ by $tw$ and expand $M_{tw}(x)$ to first order in $t$,

$$M_{tw}(x) \approx \frac{x - |x|^2 tw}{1 - 2t\langle w, x \rangle} - tw \approx x + t\left(2\langle w, x \rangle x - (1 + |x|^2)w\right).$$

The derivative of this expression at $t = 0$ (which is just the coefficient of $t$) gives us the infinitesimal generator: it is an "infinitesimal boost" of the form (3) with $Z = -2w$ and $A = 0$. Next, recall that the infinitesimal generators corresponding to the rotation components are flows of the form $\dot{x} = Ax$ for an antisymmetric matrix $A$. Together with the infinitesimal boosts, we then get all flows of the form (3). The group $G$ acts on the space $X$ in the natural way (component by component), and the infinitesimal generators of this group action on $X$ are flows of the form (1) with all $A_i$ identical. Therefore, by the general philosophy discussed earlier, the evolution of any initial point $p \in X$ under the system (1) with all $A_i = A$ lies in the group orbit $Gp$.

## III. REDUCED EQUATIONS

The given Kuramoto system has $N(d - 1)$ degrees of freedom for some large $N$. However, since the flow of the system is determined via an action of the $d(d + 1)/2$ dimensional Lie group $G$, we can alternatively study the auxiliary dynamical system on $G$, which we call the reduced equations. By ignoring rotations, we can further restrict our attention to a system on the $d$-dimensional quotient $G/SO(d) \cong \mathbb{B}^d$. The dimensional reduction not only makes the reduced equations easier to analyze than the original Kuramoto system, but the reduced equations require fewer computational resources to numerically integrate.

As before, assume all the terms $A_i$ in (1) are equal so that $A_i = A$ for some skew-symmetric matrix $A$. Fix a base point $p = (p_1, \ldots, p_N) \in X$. Then, if the points $p_i$ are in sufficiently general position, every element in the $G$-orbit of $p$ can be expressed uniquely as $gp$ for some $g \in G$, with parameters $w, z$, and $\zeta$. We wish to derive the corresponding evolution equations for $w, z$, and $\zeta$. Let $(x_i(t))$ be any solution to (1) in the group orbit $Gp$; we do not require that the initial point $(x_i(0)) = p$. Then, for $i = 1, \ldots, N$, we have $x_i(t) = g_t(p_i)$ for a unique $g_t \in G$, which determines the parameters $w, z, \zeta$ as functions of $t$. Now, consider Eq. (3), with coefficients $A$ and $Z$ evaluated at $(x_i(t))$. This is a non-autonomous ODE on $\mathbb{B}^d$, and its time-$t$ flow must be given by some $\tilde{g}_t \in G$. This ODE has solutions $x_i(t) = g_t(p_i) = g_t(g_0^{-1}(x_i(0)))$, which implies that $\tilde{g}_t = g_t g_0^{-1}$.

Therefore, for any $y_0 \in \mathbb{B}^d$,

$$y(t) = g_t(g_0^{-1}(g_0(y_0))) = g_t(y_0) = \zeta M_w(y_0) = M_{-z}(\zeta y_0)$$

must satisfy the ODE (3) with $A$ and $Z$ evaluated at $(x_i(t))$ at time $t$. In particular, if we let $y_0 = 0$, then $y(t) = -\zeta w = z$; therefore, $z$ satisfies the ODE (3).

Now, expand $y = \zeta M_w(y_0) = M_{-z}(\zeta y_0)$ to first order in $y_0$, using the variables $z$ and $\zeta$,

$$y \approx z + (1 - |z|^2)\zeta y_0;$$

therefore,

$$\dot{y} \approx \dot{z} - 2\langle \dot{z}, z \rangle \zeta y_0 + (1 - |z|^2)\dot{\zeta} y_0.$$

On the other hand, (3) gives

$$\dot{y} = Ay + \frac{1}{2}\left(1 + |y|^2\right)Z - \langle Z, y \rangle y$$

$$\approx Az + \frac{1}{2}(1 + |z|^2)Z - \langle Z, z \rangle z$$

$$+ (1 - |z|^2)\left(A\zeta y_0 + \langle z, \zeta y_0 \rangle Z - \langle Z, z \rangle \zeta y_0 - \langle Z, \zeta y_0 \rangle z\right).$$

Setting $y_0 = 0$ gives the $\dot{z}$ equation

$$\dot{z} = Az + \frac{1}{2}(1 + |z|^2)Z - \langle Z, z \rangle z \qquad (4)$$

as expected, and since $\langle Az, z \rangle = 0$, this in turn implies that

$$\langle \dot{z}, z \rangle = \frac{1}{2}(1 - |z|^2)\langle Z, z \rangle.$$

Equating the $y_0$ terms, factoring out $1 - |z|^2$, and canceling the common term $\langle Z, z \rangle \zeta y_0$ gives

$$\dot{\zeta} y_0 = A\zeta x_0 + \langle z, \zeta y_0 \rangle Z - \langle Z, \zeta y_0 \rangle z.$$

Together, the last two terms above define a special type of antisymmetric operator of $\zeta y_0$: Given any $y_1, y_2 \in \mathbb{R}^d$, define the antisymmetric operator $\alpha$ as

$$\alpha(y_1, y_2)y = \langle y_1, y \rangle y_2 - \langle y_2, y \rangle y_1;$$

this operator has range = $\text{span}(y_1, y_2)$ provided that $y_1$ and $y_2$ are linearly independent; otherwise, $\alpha(y_1, y_2) = 0$. Then, for all $y_0 \in \mathbb{R}^d$,

$$\dot{\zeta} y_0 = A\zeta y_0 + \alpha(z, Z)\zeta y_0;$$

therefore,

$$\dot{\zeta} = (A + \alpha(z, Z))\zeta.$$

Differentiating $z = -\zeta w$ gives

$$Az + \frac{1}{2}(1 + |z|^2)Z - \langle Z, z \rangle z = -\zeta \dot{w} - \dot{\zeta} w;$$

therefore,

$$\zeta \dot{w} = (A + \alpha(z, Z))z - Az - \frac{1}{2}(1 + |z|^2)Z + \langle Z, z \rangle z$$

$$= Az + |z|^2 Z - \langle Z, z \rangle z - Az - \frac{1}{2}(1 + |z|^2)Z + \langle Z, z \rangle z$$

$$= -\frac{1}{2}(1 - |z|^2)Z;$$

hence,

$$\dot{w} = -\frac{1}{2}(1 - |w|^2)\zeta^{-1}Z. \qquad (5)$$

Summing up, the evolution equations for the $(z, \zeta)$ coordinate system on $Gp$ are

$$\dot{z} = Az + \frac{1}{2}(1 + |z|^2)Z - \langle Z, z \rangle z, \qquad (6a)$$

$$\dot{\zeta} = (A + \alpha(z, Z))\zeta, \qquad (6b)$$

Chaos 31, 093113 (2021); doi: 10.1063/5.0060233       31, 093113-5
Published under an exclusive license by AIP Publishing






Chaos    ARTICLE  scitation.org/journal/cha

with $A$ and $Z$ evaluated at $M_{-z}(\zeta p)$ and for the $(w, \zeta)$ coordinate system on $Gp$ are

$$\dot{w} = -\frac{1}{2}(1 - |w|^2)\zeta^{-1}Z,$$ (7a)

$$\dot{\zeta} = (A - \alpha(\zeta w, Z))\zeta,$$ (7b)

with $A$ and $Z$ evaluated at $\zeta M_w(p)$. Note that these equations generalize the evolution equations for the parameters $w$ and $\zeta$ given in Chen et al.²⁶ for the classic case $d = 2$.

## IV. COMPARISON OF Z VS W COORDINATES

The $\dot{z}$ equation (4) is an extension of the system equation (1) on $S^{d-1}$. However, for finite $N$, the $\dot{z}$ equation does not uncouple from $\zeta$ since $Z$ is evaluated at $M_{-z}(\zeta p)$. The exception to this is in the infinite-$N$ limit: if the base point $p$ is now the uniform density on $S^{d-1}$, then $\zeta p = p$ (the uniform density is invariant under rotations) and the density $M_{-z}(p)$ is a hyperbolic Poisson density on $S^{d-1}$ whose centroid is a function of $z$. In the case $d = 2$, this Poisson density has centroid $z$. Unfortunately, this simple relationship is false for $d \geq 3$ (we will give more details on this in Sec. V).

The advantage of the $\dot{w}$ equation (5) is that for an order parameter function of the form

$$Z = \sum_{i=1}^{N} a_i x_i,$$

with $a_i \in \mathbb{R}$, $\zeta$ drops out of the $\dot{w}$ equation, and we get the reduced equation

$$\dot{w} = -\frac{1}{2}(1 - |w|^2)Z(M_w(p)).$$

The parameter $w$ essentially defines the "phase relations" among $x_i$; two configurations have the same $w$ if and only if they are related by a rotation. Therefore, $w$ is the key parameter that determines whether the system is approaching synchrony or incoherence.

The $w$ variable also has a nice invariance under change of base points. Suppose $p' = M(p) \in Gp$, then, we have coordinates $w', \zeta'$ associated with the base point $p'$. Any $q \in Gp$ has two expressions

$$q = \zeta M_w(p) = \zeta' M_{w'} p' = \zeta' M_{w'}(M(p)).$$

Assuming the coordinates of $p$ are in sufficiently general position, this implies $\zeta M_w = \zeta' M_{w'} \circ M$, and hence,

$$0 = \zeta M_w(w) = \zeta' M_{w'}(M(w)).$$

Therefore, the unique solution to $M_{w'}(y) = 0$ is $w'$, and hence, $w' = M(w)$. In other words, the coordinates $w$ and $w'$ transform exactly as the base points $p$ and $p'$.

## V. CONTINUUM LIMIT

Next, we consider the dynamics of the Kuramoto model (1) in the limit $N \to \infty$. We assume that the rotation terms $A_i = A$ are constant across the population, corresponding to identical "oscillators."

Let us also assume that the order parameter $Z$ is proportional to the centroid of the population,

$$Z = \frac{K}{N}\sum_{i=1}^{N} x_i.$$

In the continuum limit, a state of the system is a probability measure $\rho$ on $S^{d-1}$, and the order parameter becomes

$$Z = K \int_{S^{d-1}} x d\rho(x).$$

The measure $\rho$ evolves according to the continuity equation (also known as the noiseless Fokker–Planck equation) associated with the flow in (1). Naturally, this flow must preserve group orbits under the action of $G$. Recall that if $M \in G$, then the measure $M_*\rho$ is defined by the adjunction formula

$$\int_{S^{d-1}} f(x) d(M_*\rho)(x) = \int_{S^{d-1}} f(M(x)) d\rho(x).$$

In particular, we can consider the $G$-orbit of the uniform probability measure $\sigma$ on $S^{d-1}$. This orbit is special; whereas a typical group orbit $G\rho$ has dimension equal to the dimension of $G$, namely, $d(d + 1)/2$, the orbit $G\sigma$ has dimension only $d$. This is because the stabilizer of $\sigma$ is $SO(d)$; any rotation fixes $\sigma$, whereas the boosts deform $\sigma$. Hence, the orbit $G\sigma$ has dimension $d$. Any element in $G\sigma$ can be written as $(M_{-z})_*\sigma$, with $z \in B^d$. The evolution equation for $z$ is (4), with

$$Z(z) = K \int_{S^{d-1}} x d(M_{-z})_*\sigma(x) = K \int_{S^{d-1}} M_{-z}(x) d\sigma(x).$$ (8)

In the case $d = 2$ with $x = \zeta \in S^1$, we have

$$d\sigma(\zeta) = \frac{1}{2\pi i} \frac{d\zeta}{\zeta};$$

therefore, the integral

$$Z(z) = \frac{K}{2\pi i} \int_{S^1} \frac{\zeta + z}{1 + z\zeta} \cdot \frac{d\zeta}{\zeta} = K \frac{\zeta + z}{1 + z\zeta}\bigg|_{\zeta=0} = Kz$$

by the Cauchy integral formula. Therefore, (4) simplifies to the equation

$$\dot{z} = i\omega z + \frac{K}{2}(1 - |z|^2)z$$

when $d = 2$. Unfortunately, the formula $Z(z) = Kz$ is not correct for $d \geq 3$; though as we shall see later, this formula is correct in higher dimensions for the complex hyperbolic model in even dimensions, which we discuss in Sec. VI. For $d = 2$, the two geometries agree, which explains the coincidence for $d = 2$.

Any Riemannian manifold $X$ has a Laplace–Beltrami operator $\Delta$ associated with its metric; functions $f$ on $X$ satisfying the equation $\Delta f = 0$ are called harmonic. For functions on the ball $B^d$ with the

Chaos 31, 093113 (2021); doi: 10.1063/5.0060233       31, 093113-6
Published under an exclusive license by AIP Publishing






Chaos    ARTICLE  scitation.org/journal/cha

hyperbolic metric, this operator is

$$\Delta_{hyp} = (1 - |x|^2)^2 \Delta_{euc} + 2(d - 2)(1 - |x|^2) \sum_{i=1}^{d} x_i \frac{\partial}{\partial x_i},$$

where

$$\Delta_{euc} = \sum_{i=1}^{d} \frac{\partial^2}{\partial x_i^2}$$

is the standard Laplace operator (see Stoll,<sup>48</sup> Chap. 3). We will call solutions to the equation $\Delta_{hyp}f = 0$ *hyperbolic harmonic functions*; for $d = 2$, these coincide with ordinary (Euclidean) harmonic functions. We can consider the hyperbolic analog of the classical Dirichlet problem: given a continuous function $f$ on $S^{d-1}$, extend $f$ to a hyperbolic harmonic function $\tilde{f}$ on $B^d$. Assuming that this problem has a unique solution, then for any rotation $\zeta \in SO(d)$, we must have $\tilde{f} \circ \zeta = \tilde{f} \circ \zeta$ since rotations preserve the hyperbolic metric. If we average $\tilde{f} \circ \zeta$ on $S^{d-1}$ over all rotations $\zeta \in SO(d)$, we get the constant function

$$f_{ave} = \int_{S^{d-1}} f(x) d\sigma(x)$$

on $S^{d-1}$, and any constant is hyperbolic harmonic on $B^d$. Therefore, the average on $B^d$ of $\tilde{f} \circ \zeta = \tilde{f} \circ \zeta$ over all $\zeta \in SO(d)$ must be the constant $f_{ave}$. However, $\tilde{f}(\zeta(0)) = \tilde{f}(0)$ for all $\zeta$; therefore, we must have

$$\tilde{f}(0) = \int_{S^{d-1}} f(x) d\sigma(x).$$

Now, let $z \in B^d$; since $M_{-z}$ preserves the hyperbolic metric, we must have $\tilde{f} \circ M_{-z} = \tilde{f} \circ M_{-z}$, which implies

$$\tilde{f}(z) = \tilde{f} \circ M_{-z}(0)$$

$$= \int_{S^{d-1}} f(M_{-z}(x)) d\sigma(x)$$

$$= \int_{S^{d-1}} f(x) d((M_{-z})_* \sigma)(x).$$

As shown in Chap. 5 of Stoll,<sup>48</sup> the measure $(M_{-z})_* \sigma$ is given by the formula

$$d((M_{-z})_* \sigma)(x) = P_{hyp}(z, x) d\sigma(x),$$

with hyperbolic Poisson kernel function

$$P_{hyp}(z, x) = \left(\frac{1 - |z|^2}{|z - x|^2}\right)^{d-1}.$$ (9)

Thus, the solution to the hyperbolic Dirichlet problem with boundary function $f$ on $S^{d-1}$ is given by the hyperbolic Poisson integral

$$\tilde{f}(z) = \int_{S^{d-1}} P_{hyp}(z, x)f(x) d\sigma(x), \quad z \in B^d.$$

The orbit $G\sigma$ consists of all *hyperbolic* Poisson measures $P(z, x) d\sigma(x)$, parameterized by $z \in B^d$. By contrast, the Euclidean Poisson kernel function is

$$P_{euc}(z, x) = \frac{1 - |z|^2}{|z - x|^d};$$

therefore, the hyperbolic Poisson measures agree with the Euclidean Poisson measures only if $d = 2$.

Now, we can calculate the expression $Z(z)$ in the general case $d \geq 2$. We see from (8) that $Z(z)$ is the hyperbolic Poisson integral of the function $Kx$ on $S^{d-1}$. The function $Kx$ is (Euclidean) harmonic and homogeneous of degree 1 on $\mathbb{R}^d$; following the recipe in Chap. 5 of Stoll,<sup>48</sup> we see that its extension from $S^{d-1}$ to a hyperbolic harmonic function on $B^d$ is given by

$$Z(z) = K \frac{F(1, 1 - d/2; 1 + d/2; |z|^2)}{F(1, 1 - d/2; 1 + d/2; 1)} z,$$ (10)

where $F$ is the hypergeometric function

$$F(a, b; c; t) = \sum_{k=0}^{\infty} \frac{(a)_k (b)_k}{(c)_k} \frac{t^k}{k!},$$

with $(a)_0 = 1$ and $(a)_k = a(a + 1) \cdots (a + k - 1)$ for $k \geq 1$. Notice that if $a$ or $b = 0$, then $F(a, b; c; t) = 1$; this gives $Z(z) = Kz$ for $d = 2$, as expected.

## VI. COMPLEX CASE

There is an alternative generalization of Kuramoto networks to higher-dimensional oscillators when $d = 2m$ is even. Then, $\mathbb{R}^d = \mathbb{C}^m$, and we can study systems of the form

$$\dot{x}_j = A_j x + Z - \langle x_j, Z \rangle x_j, \quad i = 1, \ldots, N,$$ (11)

where now $x_i$ is a point on the unit sphere $S^{2m-1} \subset \mathbb{C}^m$, $A_i$ is an anti-Hermitian $m \times m$ complex matrix, $Z \in \mathbb{C}^m$, and $\langle, \rangle$ denotes the complex-valued Hermitian inner product. These systems are the same as the real case when $d = 2, m = 1$ but are different for $m \geq 2$. To see this, suppose

$$Ax + Y - \langle x, Y \rangle_{\mathbb{R}} x = Bx + Z - \langle x, Z \rangle_{\mathbb{C}} x$$

for all $x \in S^{2m-1} \subset \mathbb{C}^m = \mathbb{R}^d$, where $A$ is antisymmetric, $B$ is anti-Hermitian, $Y, Z \in \mathbb{C}^m$, and we use the subscripts $\mathbb{R}$ and $\mathbb{C}$ to distinguish the real and complex inner products. Then,

$$(A - B)x = Z - Y + \left(\langle x, Y \rangle_{\mathbb{R}} - \langle x, Z \rangle_{\mathbb{C}}\right)x,$$

and therefore, $(A - B)(-x) = (A - B)x$ for all $x \in S^{2m-1}$, which implies $A = B$. This implies

$$Y - Z = \left(\langle x, Y \rangle_{\mathbb{R}} - \langle x, Z \rangle_{\mathbb{C}}\right)x$$

for all $x \in S^{2m-1}$; hence, $Y - Z \in \text{span}_{\mathbb{C}}(x)$ for all $x \in \mathbb{C}^m$; if $m \geq 2$, this implies $Y = Z$. However, then we have

$$\langle x, Y \rangle_{\mathbb{R}} = \langle x, Y \rangle_{\mathbb{C}}$$

for all $x \in \mathbb{C}^m$, which can only hold if $Y = 0$. Hence, for $m \geq 2$, the only flows simultaneously of the form (1) and (11) have $Z = 0$ and $A$ anti-Hermitian.

Flows of the form (11) are related to the complex hyperbolic geometry on the complex unit ball $B^m$ with the Bergman metric

Chaos 31, 093113 (2021); doi: 10.1063/5.0060233       31, 093113-7
Published under an exclusive license by AIP Publishing






Chaos    ARTICLE  scitation.org/journal/cha

(see Rudin,⁴⁹ Chap. 1). The orientation-preserving isometries of this metric are generated by unitary transformations $\zeta \in U(m)$ and boost transformations of the form

$$M_w(x) = \frac{\sqrt{1 - |w|^2} x + \left(\frac{\langle x,w \rangle}{1+\sqrt{1-|w|^2}} - 1\right) w}{1 - \langle x, w \rangle}$$

$$= \frac{x - w + \frac{\langle x,w \rangle w - |w|^2 x}{1+\sqrt{1-|w|^2}}}{1 - \langle x, w \rangle}.$$

Notice that when $m = 1$, this reduces to the standard complex Möbius map $M_w$. As in the real case, $M_0$ is the identity, $M_{-w}^{-1} = M_w$, $M_w(w) = 0$, and $M_w(0) = -w$. Any orientation-preserving isometry of $B^m$ can be expressed uniquely in the form

$$g(x) = \zeta M_w(x) = M_{-z}(\xi x),$$

where $w, z \in B^m$, but now $\zeta, \xi \in U(m)$, the complex unitary group. Linearizing at $x = 0$ gives

$$g(x) \approx \zeta \left(-w - \langle x, w \rangle w + \sqrt{1 - |w|^2} x + \frac{\langle x, w \rangle w}{1 + \sqrt{1 - |w|^2}}\right)$$

$$\approx \zeta \left(-w + \sqrt{1 - |w|^2} x - \frac{\sqrt{1 - |w|^2}\langle x, w \rangle w}{1 + \sqrt{1 - |w|^2}}\right)$$

$$\approx z + \sqrt{1 - |z|^2} \xi x - \frac{\sqrt{1 - |z|^2}\langle \xi x, z \rangle z}{1 + \sqrt{1 - |z|^2}},$$

which implies $z = -\zeta w$ (hence $|z| = |w|$) and $\xi = \zeta$, as before. The corresponding infinitesimal transformations are given by flows on the complex unit ball $B^m$ of the form

$$\dot{y} = Ay + Z - \langle y, Z \rangle y, \qquad (12)$$

with $A$ being anti-Hermitian $m \times m$ and $Z \in \mathbb{C}^m$. This flow extends to a flow on $S^{2m-1}$ of the form in (11). Note the absence of the quadratic term $|y|^2Z$ here. To derive these infinitesimal transformations, we can apply our prior power series expansion, noting that $|x| = 1$ to obtain

$$M_{tw}(x) \approx \frac{x - tw}{1 - t\langle x, w \rangle} \approx x + 2 (\langle x, w \rangle x - w) t.$$

Therefore, the infinitesimal generator is an "infinitesimal boost" of the form (12) with $Z = -\frac{1}{2}w$ and $A = 0$. The infinitesimal generators corresponding to the rotation components are flows of the form $\dot{x} = Ax$ with $A$ being anti-Hermitian; together with the infinitesimal boosts, we get all flows of the form (12).

We mention in passing that the complex model (11) has a natural quantum network analog, studied by Lohe,²⁹ in which the complex vector $x_i$ is replaced by a normalized wave function $|\psi_i\rangle$. We expect that our reduction techniques extend to this infinite-dimensional quantum model.

## VII. RELATION TO PREVIOUS RESEARCH

Many of the results above can be found in some form in the work of earlier authors.²⁸,³⁰,³¹,³⁴⁻³⁹,⁴³,⁴⁴ Three papers, in particular, overlap considerably with the present work.

Tanaka³⁰ demonstrates in his 2014 paper that the dynamics of (1) can be reduced using Möbius transformations that fix the unit ball, similar to what Marvel et al. found²⁴ for the traditional Kuramoto model. Tanaka writes his Möbius transformations differently from ours, but he uses the same group of transformations and he also gets reduced equations for his Möbius parameters. Tanaka's equation (10b) looks similar to our $\dot{z}$ equation (4), except without the $|z|^2$ term, which is puzzling. He does not mention the reduction down to dimension $d$ in the finite-$N$ case that we get with the $\dot{w}$ equation (5). Tanaka also notes the complex case when $d = 2m$ is different and generalizes the Ott–Antonsen residue calculation to this case, which is the highlight of his paper. In the real case, Tanaka's equation (15) is similar to our equation (10), though we were not able to show that the two expressions are equivalent. Finally, Tanaka also presents a generalization of the Ott–Antonsen reduction²¹ for the complex version of the system.

Lohe³⁵ also looks at the same system as (1) [see his Eq. (22)], and he derives a similar reduction as ours by using Möbius transformations for the finite-$N$ model. His transformation (30) on $S^{d-1}$ is our $M_w$ (with $v = w$) and his Eq. (31) is the same as our $\dot{z}$ equation (4). He also has something that looks like the $\dot{w}$ equation (5), which he says is independent of the rest of the reduced system for (in our notation) an order parameter function of the form

$$Z = \frac{1}{N} \sum_{i=1}^{N} \lambda_i Q_i x_i,$$

where $Q_i \in O(d)$ and $\lambda_i \in \mathbb{R}$. However, such a $Z$ does not satisfy the identity $\zeta Z(p) = Z(\zeta p)$ for all rotations $\zeta$ unless $Q_i = \pm I$; therefore, we do not see how the $\zeta$ term cancels.

Lohe's map $M$ in his Eq. (55) (ignoring the $R$ factor) agrees with our map $M_{-v}$ on the sphere $S^{d-1}$, but not on the ball $B^d$. Therefore, it is not a Möbius transformation of the type we are using. For example, $M(-v) = v$, whereas $M_{-v}(-v) = 0$. We are not sure why Lohe³⁵ prefers these maps over the boosts; he claims that $M$ preserves cross-ratios, but we do not see why this is advantageous. His map $F$ in Eq. (63) (again ignoring the $R$ factor) is exactly our $M_{-v}$.

Chandra et al.³⁸ concentrate on the infinite-$N$ or continuum limit system and derive a dynamical reduction for a special class of probability densities on $S^{d-1}$, generalizing the Poisson densities used in the Ott–Antonsen reduction. They proceed directly to the infinite-$N$ version of (1). They make a very clever guess [their Eq. (7)] of the form of the special densities that generalize the Poisson densities for $d = 2$ and then calculate the exponent in the denominator of their expression, getting exactly the hyperbolic Poisson kernel densities in (9) above. Their Eq. (15) is exactly the same as our $\dot{z}$ equation (4) in the infinite-$N$ limit. The integral in their Eq. (19) can be evaluated, as shown above in (10).

## VIII. AN EXAMPLE: A FIRST-ORDER LINEAR ORDER PARAMETER GIVES A GRADIENT SYSTEM

We conclude with an analysis of the system (1) with a weighted order parameter

$$Z = \sum_{i=1}^{N} a_i x_i, \qquad (13)$$

where the $a_i$ are real constants.

Chaos 31, 093113 (2021); doi: 10.1063/5.0060233       31, 093113-8
Published under an exclusive license by AIP Publishing






Chaos    ARTICLE  scitation.org/journal/cha

## A. Computer visualization

We have been discussing a system of particles on a sphere that coalesce (in other words, they spontaneously synchronize) when certain conditions are met. To visualize this coalescence, we implemented the Runge–Kutta algorithm to numerically solve the Kuramoto model on the two-dimensional sphere in a three-dimensional space corresponding to the case $d = 3$ in (1). For simplicity, we simulated $N = 100$ particles with equal weights ($a_i = 1/N$ in the order parameter $Z$) and set the rotation term $A$ to zero for all the particles, a choice that is tantamount to ignoring the rotational influence, or equivalently, rotating the frame of reference along with the entire system as it evolves.

In the simulation shown in Fig. 1, randomly chosen points on the sphere were used as initial conditions. As time increases, one can see that the particles coalesce to a limit point, mimicking the spontaneous synchronization that is well known for the traditional Kuramoto model ($d = 2$) when the oscillators are identical.

Later in this section, we will prove that this synchronization behavior holds more generally for Kuramoto models on the sphere having weighted order parameters of the form (13), provided that the weights $a_i$ are all positive and sum to 1 and no individual weight exceeds 1/2. A partial result in this direction was obtained by Choi and Ha.³² These authors prove that in the case where all $a_i$ are equal and positive, initial conditions satisfying $\langle x_i(0), Z(0)\rangle > 0$ for all $i$ will synchronize. Geometrically, this condition is equivalent to requiring that the oscillators all lie on the hemisphere given by $\langle x_i, y\rangle > 0$ for some vector $y = 0$. More generally, similar partial synchronization results are obtained by Ha et al.⁴³ for the case

$$Z = (aI + W) \sum_{i=1}^{N} x_i,$$

with $W$ skew-symmetric and by Ha and Park⁴¹ for the complex system (11) with $Z = \sum x_i$.

Figure 2 shows a simulation in which we weighted each particle according to the terms in a Riemann sum approximating the integral of the normal probability distribution. The pink particles, which have higher weights, exert greater influence over the final synchronization location of the particles, but there is still synchronization.

When time runs backward, almost all initial conditions of the particles tend toward a limiting configuration where their centroid is at the origin. The exception is when we have a majority cluster, depicted in Fig. 3, where one particle has a weight that exceeds the weight of all other particles. When this condition holds, it is impossible to arrange the particles so that their weighted centroid is at the origin, so the backward-time limit will tend toward an antipodal configuration, where all particles not in the majority cluster will coalesce around the antipode of the cluster. This is the configuration that minimizes the magnitude of the weighted centroid. We do not include the proof that the backward-time limit is antipodal in this case, but it is a straightforward generalization of the result that we do prove below.

## B. Existence of a hyperbolic gradient

As mentioned above, if $Z$ has the form in (13), then the $\dot{w}$ equation in (5) reduces to

$$\dot{w} = -\frac{1}{2}(1 - |w|^2)Z(M_w(p))$$ (14)

independent of the parameter $\zeta$. We will show that this is a gradient flow on the unit ball $B^d$ with respect to the hyperbolic metric. In the presence of a Riemannian metric, we can associate a 1-form to any vector field, and the vector field is gradient if and only if the associated 1-form is exact; since the unit ball is simply connected, this holds if and only if the associated 1-form is closed. For the Euclidean metric on $B^d$ (or any open subset of $\mathbb{R}^d$) and standard coordinates $w_1, \ldots, w_d$, the 1-form associated with the vector field with components $f_1, \ldots, f_d$ is

$$\omega = f_1 dw_1 + \cdots + f_d dw_d.$$

![Three spheres showing the evolution of a Kuramoto system at different time points. The first sphere (t=0) shows particles distributed around the surface with varying sizes. The second sphere (t=10) shows particles clustering together. The third sphere (t=40) shows particles coalesced into a single region.]

**FIG. 1.** A first-order linear Kuramoto system on the two-dimensional sphere $S^2$ with equal weights $a_i = 1/N$ and randomly chosen initial conditions. The states shown are at $t = 0$, $t = 10$, and $t = 40$, respectively. This simulation was written in Python and visualized with Plotly.

Chaos 31, 093113 (2021); doi: 10.1063/5.0060233       31, 093113-9
Published under an exclusive license by AIP Publishing






Chaos
Chaos 31, 093113 (2021); doi: 10.1063/5.0060233       31, 093113-10
Published under an exclusive license by AIP Publishing

**FIG. 2.** A first-order linear Kuramoto system on $S^2$ with weights distributed according to a Riemann sum, which approximates the integral of a normal distribution and randomly chosen initial conditions. Pink particles contribute to the order parameter with greater weights than the blue particles do. The states shown are at $t = 0$, $t = 10$, and $t = 40$, respectively.

If we scale the Euclidean metric by a positive smooth function $\phi$, then the associated 1-form with respect to the metric $ds = \phi|dw|$ is

$$\omega = \phi^2(f_1 dw_1 + \cdots + f_d dw_d).$$

Therefore, the gradient of a function $\Phi$ with respect to this scaled metric is given by

$$\nabla\Phi = \phi^{-2}\nabla_{euc}\Phi,$$

where $\nabla_{euc}$ denotes the ordinary Euclidean gradient operator. We have $\phi(w) = 2(1 - |w|^2)^{-1}$ for the hyperbolic metric; therefore, the hyperbolic gradient operator on $B^d$ is given by

$$\nabla_{hyp}\Phi(w) = \frac{1}{4}(1 - |w|^2)^2\nabla_{euc}\Phi(w).$$

Now, let us consider the vector field $V$ defined by (14). By linearity, it suffices to treat the case $Z = x_i$, and we can take $i = 1$ without loss of generality. Then, the associated 1-form is

$$\omega = \frac{4}{(1 - |w|^2)^2}\left(-\frac{1}{2}(1 - |w|^2)\right)$$

$$\cdot \sum_{j=1}^{d}\left(\frac{(1 - |w|^2)(p_{1,j} - w_j)}{|p_1 - w|^2} - w_j\right)dw_j$$

$$= -2\sum_{j=1}^{d}\left(\frac{p_{1,j} - w_j}{|p_1 - w|^2} - \frac{w_j}{1 - |w|^2}\right)dw_j,$$

**FIG. 3.** A first-order linear Kuramoto system on $S^2$ with a majority cluster, where one particle is chosen to have a weight that exceeds the combined weights of all other particles (or equivalently, where all the particles have equal weight, but a majority of them cluster into a single point and, therefore, act if they were a single giant particle, hence the name "majority cluster"). The states shown are at $t = 0$, $t = -10$, and $t = -40$, respectively; we have chosen to depict time running backward to highlight that the backward-time limit tends toward an antipodal configuration. In this simulation, one particle, depicted in pink, was chosen to have a weight of 0.6, and the remaining 99 particles, depicted as blue, were chosen to have equal weights of 0.4/99.





Chaos    ARTICLE    scitation.org/journal/cha

where $p_{1,j}$ denotes the $j$th component of the point $p_1 \in S^{d-1}$. Let $E_j$ denote the coefficient of $dw_j$ in parentheses above; then,

$$d\omega = -2 \sum_{j,k=1}^{d} \frac{\partial E_j}{\partial w_k} dw_k \wedge dw_j.$$

Applying the chain and quotient rules gives

$$\frac{\partial E_j}{\partial w_k} = \frac{2(p_{1,j} - w^j)(p_{1,k} - w^k)}{|p_1 - w|^4} + \frac{2w_j w_k}{(1 - |w|^2)^2}$$

for $j \neq k$, which is symmetric in $j$ and $k$; hence, the sum above for $d\omega$ simplifies to $d\omega = 0$. Thus, $\omega$ is closed, and we see that the flow (14) is gradient for any order parameter function of the form (13).

Next, we show that the hyperbolic potential for $V$, up to an additive constant, is given by

$$\Phi(w) = \sum_{i=1}^{N} a_i \log \frac{1 - |w|^2}{|w - p_i|^2} = \frac{1}{d-1} \sum_{i=1}^{N} a_i \log P_{\text{hyp}}(w, p_i). \quad (15)$$

Here, we follow the convention that the potential *decreases* along trajectories; therefore, we are asserting that $\nabla_{\text{hyp}} \Phi = -V$. To derive this, we use the identity $\nabla_{\text{euc}} |w - w_0|^2 = 2(w - w_0)$ for any constant vector $w_0 \in \mathbb{R}^d$. Then,

$$\nabla_{\text{euc}} \Phi(w) = \sum_{i=1}^{N} a_i \left( -\frac{2w}{1 - |w|^2} - \frac{2(w - p_i)}{|w - p_i|^2} \right)$$

$$= \frac{2}{1 - |w|^2} \sum_{i=1}^{N} a_i \left( \frac{(1 - |w|^2)(p_i - w)}{|w - p_i|^2} - w \right)$$

$$= \frac{2}{1 - |w|^2} \sum_{i=1}^{N} a_i M_w(p_i) = \frac{2}{1 - |w|^2} Z(M_w(p)).$$

Hence, we see that

$$\nabla_{\text{hyp}} \Phi(w) = \frac{1}{2}(1 - |w|^2) Z(M_w(p)) = -V(w),$$

as desired.

## C. Analysis of dynamics

We can use the existence of the potential $\Phi(w)$ for the flow on $B^d$ to prove a global synchrony result for the system (1) when the coefficients $a_i$ in the order parameter $Z$ are all positive. Specifically, we assume that $0 < a_i < 1/2$ for all $i$, and $\sum_{i=1}^{N} a_i = 1$. We also assume $N \geq 3$, and all the rotation terms $A_i$ in (1) are equal. Under these conditions, almost all trajectories for (1) converge in forward time to the $(d - 1)$-dimensional diagonal manifold $\Delta \subset X$ as $t \to \infty$, meaning that the system self-synchronizes. In contrast, in backward time, the system tends to an incoherent state having zero order parameter: as $t \to -\infty$, almost all trajectories for (1) converge to the codimension-$d$ subspace $\Sigma \subset X$ consisting of states with $Z(p) = 0$.

The proof is modeled after Theorem 1 in Chen *et al.*$^{27}$ and will be based on two preliminary lemmas. In each of these lemmas, we assume the conditions on the $a_i$ above and that the base point $p = (p_i)$ for the flow (14) has all distinct coordinates.

We begin with a general observation about gradient flows in the ball $B^d$: if $w_0 \in B^d$ is any initial condition and $w^* \in B^d$ is in the forward limit set $\Omega_+(w_0)$, then $w^*$ is a fixed point for the flow. To see this, let $\Phi$ be a potential for the flow, and suppose $w(t_n) \to w^* \in B^d$ for some sequence $t_n \to \infty$. Since the potential decreases along trajectories,

$$\lim_{t \to \infty} \Phi(w(t)) = \lim_{n \to \infty} \Phi(w(t_n)) = \Phi(w^*).$$

Let $F_t$ denote the time-$t$ flow map. If $w^*$ is not a fixed point, then for any $s > 0$,

$$\lim_{t \to \infty} \Phi(w(t)) = \lim_{n \to \infty} \Phi(w(t_n + s))$$

$$= \lim_{n \to \infty} \Phi(F_s(w(t_n)))$$

$$= \Phi(F_s(w^*))$$

$$< \Phi(w^*),$$

which is a contradiction; therefore, $w^*$ must be a fixed point. (Compact limit sets are connected; therefore, $\Omega_+(w_0)$ cannot consist of two or more but finitely many fixed points; however, it is possible that forward or backward limit sets for gradient flows consist of a continuum of fixed points. We will see that this is not the case for our system on $B^d$.

**Lemma 1:** *Any fixed point for the flow* (14) *in* $B^d$ *is repelling.*

*Proof.* Suppose $w^* \in B^d$ is a fixed point for (14). As discussed above, an advantage of using the $w$-parameter is the equivariance with respect to change of base point $p$. Consequently, we can assume $w^* = 0$ without loss of generality; therefore, $Z(p) = \sum_{i=1}^{N} a_i p_i = 0$. To first order in $w$,

$$M_w(p_i) = \frac{p_i - w}{1 - 2\langle w, p_i \rangle} - w$$

$$= (p_i - w)(1 + 2\langle w, p_i \rangle) - w$$

$$= p_i - 2w + 2\langle w, p_i \rangle p_i.$$

The linearization of (14) at the fixed point $w^* = 0$ is

$$\dot{w} = -\frac{1}{2} \sum_{i=1}^{N} a_i (p_i - 2w + 2\langle w, p_i \rangle p_i)$$

$$= w - \sum_{i=1}^{N} a_i \langle w, p_i \rangle p_i.$$

We claim that the linear map

$$Tw = \sum_{i=1}^{N} a_i \langle w, p_i \rangle p_i$$

has $||T|| < 1$; to see this, suppose $|w| = 1$. Then, $|\langle w, p_i \rangle p_i| \leq 1$, and $Tw$ is a convex combination of the vectors $\langle w, p_i \rangle p_i$. We can only obtain $|Tw| = 1$ if all terms $\langle w, p_i \rangle p_i = u$ with $|u| = 1$, which implies all $p_i = \pm u$, and this cannot happen if at least three of the $p_i$ are distinct. Hence, $||T|| < 1$, and therefore, the eigenvalues $\mu_i$ of $T$ satisfy $|\mu_i| < 1$. The eigenvalues for the $\dot{w}$ linearization are $\lambda_i = 1 - \mu_i$; therefore, we see that $\text{Re } \lambda_i > 0$ for all $i$, establishing that the fixed point $w^*$ is repelling. $\square$

Chaos 31, 093113 (2021); doi: 10.1063/5.0060233       31, 093113-11
Published under an exclusive license by AIP Publishing






Chaos    ARTICLE    scitation.org/journal/cha

**Lemma 2:** $\lim_{|w| \to 1} \Phi(w) = -\infty$.

*Proof.* It suffices to show that

$$\lim_{n \to \infty} \Phi(w_n) = -\infty$$

for any sequence $w_n \in B^d$ with $w_n \to x \in S^{d-1}$. The result is clear if $x \neq p_i$: as $n \to \infty$, the terms $|w_n - p_i|$ in the potential (15) are bounded away from 0 and $1 - |w_n|^2 \to 0$. Therefore, let us say that $w_n \to p_1$. We rewrite $\Phi(w_n)$ as

$$\Phi(w_n) = \log(1 - |w_n|^2) - 2a_1 \log |w_n - p_1|$$

$$- 2 \sum_{i=2}^N a_i \log |w_n - p_i|$$

$$= \log(1 - |w_n|) - 2a_1 \log |w_n - p_1|$$

$$+ \log(1 + |w_n|) - 2 \sum_{i=2}^N a_i \log |w_n - p_i|.$$

The latter two terms above have finite limit as $n \to \infty$; therefore, we focus on the first two terms. We have $1 - |w_n| \leq |w_n - p_1|$; therefore,

$$\log(1 - |w_n|) - 2a_1 \log |w_n - p_1| \leq (1 - 2a_1) \log |w_n - p_1| \to -\infty$$

as $n \to \infty$, which proves our result. Notice that we need the assumption $a_i < 1/2$ for this argument. □

**Theorem:** *Under the conditions above, almost all trajectories for* (1) *converge to* $\Delta$ *as* $t \to \infty$ *and to* $\Sigma$ *as* $t \to -\infty$.

*Proof.* Let $p = (p_1, \ldots, p_N) \in X$ be any point with all distinct coordinates. The points on $Gp$ are parameterized by $w \in B^d$ and $\zeta \in SO(d)$, and the dynamics for these parameters are given by (5). We begin with the dynamics as $t \to -\infty$. Let $w(t)$ be a trajectory for (14) with initial condition $w_0 \in B^d$ and consider the backward-time limit set $\Omega_-(w_0)$; this is a nonempty, compact, connected subset of $\overline{B^d}$. The potential $\Phi$ is decreasing along all trajectories $w(t)$, hence bounded below as $t \to -\infty$; therefore, Lemma 2 implies that the limit set $\Omega_-(w_0)$ must be contained in the interior $B^d$. We know that any $w^* \in \Omega_-(w_0)$ is a fixed point for the flow. By Lemma 1, $w^*$ is repelling, and therefore, any trajectory $w(t)$ that comes sufficiently close to $w^*$ must have $w(t) \to w^*$ as $t \to -\infty$; therefore, $\Omega_-(w_0) = \{w^*\}$. This proves the existence of fixed points for (14) and that every trajectory $w(t)$ converges to a fixed point as $t \to -\infty$. If the flow had multiple fixed points, we would obtain a partition of $B^d$ into the disjoint open basins of repulsion of the fixed points, violating connectedness of the ball. Therefore, (14) has a unique fixed point $w^*$, and $w(t) \to w^*$ as $t \to -\infty$ for all trajectories. The fixed point $w^*$ has $Z(M_{w^*}(p)) = 0$; therefore, all trajectories in $Gp$ converge to $\Sigma$ as $t \to -\infty$.

In forward time, the limit set $\Omega_+(w_0)$ for any $w_0 \neq w^*$ must be completely contained in the boundary $S^{d-1}$ since the unique fixed point $w^* \in B^d$ is repelling. Suppose we remove the factor $(1/2)$ $(1 - |w|^2)$ in the flow (14); the scaled vector field on $B^d$ given by

$$\dot{w} = -\sum_{i=1}^N a_i M_w(p_i) = w - \sum_{i=1}^N a_i \left( \frac{(1 - |w|^2)(p_i - w)}{|p_i - w|^2} \right)$$ (16)

has the same trajectories as the original flow, just with different time parameterizations. Observe that this scaled vector field extends smoothly to $\mathbb{R}^d - \{p_i\}$ and coincides with the radial vector field $x$ at any $x \in S^{d-1}$ with $x \neq p_i$. Therefore, there is a unique trajectory passing through each point $x \in S^{d-1}$, flowing from the interior to the exterior of the sphere, as long as $x \neq p_i$. Consequently, the original flow (14) has a unique trajectory $w(t)$ in $B^d$ with $w(t) \to x$ as $t \to \infty$, as long as $x \neq p_i$. This also shows that there is a neighborhood $U$ of $S^{d-1} - \{p_i\}$ such that if $w(t_0) \in U$ for some $t_0$, then $w(t) \to x$ $\neq p_i$ for some $x \in S^{d-1}$. Therefore, if $\Omega_+(w_0)$ contains some $x \neq p_i$, then the trajectory $w(t)$ of $w_0$ must enter the neighborhood $U$, and therefore, $w(t) \to x \in S^{d-1}$ as $t \to \infty$.

Since limit sets are connected, the only other possibility is $\Omega_+(w_0) = \{p_i\}$ for some $i$; equivalently, $w(t) \to p_i$. We will show that there is a unique trajectory with this behavior for each $p_i$. Assuming this, we see that with $N + 1$ exceptions, any trajectory $w(t)$ converges to a point $x \in S^{d-1}$ with $x \neq p_i$ (the exceptions are the $N$ trajectories converging to the base point coordinates $p_i$ and the fixed point trajectory $w^*$). The corresponding trajectory in $Gp$ has coordinates

$$\zeta(t)M_{w(t)}(p_i) = \zeta(t) \left( \frac{(1 - |w(t)|^2)(p_i - w(t))}{|p_i - w(t)|^2} - w(t) \right).$$

We have $|w(t)| \to 1$ and $|p_i - w(t)|$ is bounded away from 0 as $t \to \infty$; as a result, $M_{w(t)}(p_i) \to -x$ for each $i$; therefore, the trajectory $\zeta(t)M_{w(t)}(p)$ in $Gp$ converges to $\Delta$ as $t \to \infty$.

This analysis breaks down at $x = p_i$ because the scaled vector field above does not have a unique limit as $w \to p_i$; alternatively, its limit depends on the direction of the approach. To see this, write $w = p_1 - ru$, where $0 < r < 1$ and $|u| = 1$ (with this convention, $u = p_1$ corresponds to $w$ approaching $p_1$ radially). Then, $|p_1 - w|$ $= r$ and

$$|w|^2 = 1 - 2r\langle p_1, u \rangle + r^2;$$

therefore,

$$\frac{(1 - |w|^2)(p_1 - w)}{|p_1 - w|^2} = \frac{(2r\langle p_1, u \rangle - r^2)ru}{r^2} = (2\langle p_1, u \rangle - r) u.$$

As $r \to 0$, the magnitude of this term is $2\langle p_1, u \rangle$, which depends on the angle of approach given by $u$ (note that $\langle p_1, u \rangle > 0$ because $u$ points outward at $p_1$).

To complete the proof, we will examine the scaled system (16) using the polar representation $(r, u)$ and show that the polar system has the unique fixed point $r^* = 0, u^* = p_1$, which has a unique attracting trajectory because it is a saddle with a $(d - 1)$-dimensional unstable manifold.

We see that the scaled system has

$$\dot{w} = p_1 - ru - a_1 (2\langle p_1, u \rangle - r) u + O(r)$$

$$= p_1 - 2a_1\langle p_1, u \rangle u + O(r),$$

where the $O(r)$ term is a smooth function of $r$ and $u$ for $|r| < \varepsilon$ $= \min |p_i - p_1|$, $i \geq 2$. This condition ensures that $|p_i - w|$ $\geq |p_i - p_1| - |r| > 0$; therefore, the $i \geq 2$ terms in the scaled $\dot{w}$ equation are all smooth functions of $r$ and $u$. Also, we can allow $r < 0$ here, even though it is not relevant to the $\dot{w}$ system. Now,

Chaos 31, 093113 (2021); doi: 10.1063/5.0060233    31, 093113-12
Published under an exclusive license by AIP Publishing






Chaos    ARTICLE  scitation.org/journal/cha

$$r^2 = |w - p_1|^2$$; therefore,

$$\dot{r}r = \langle w - p_1, \dot{w} \rangle = -r\langle u, \dot{w} \rangle,$$

which gives

$$\dot{r} = -(1 - 2a_1)\langle p_1, u \rangle + O(r).$$

Differentiating $$ru = p_1 - w$$ gives

$$r\dot{u} = -\dot{r}u - \dot{w}$$

$$= (1 - 2a_1)\langle p_1, u \rangle u - (p_1 - 2a_1\langle p_1, u \rangle u) + O(r)$$

$$= \langle p_1, u \rangle u - p_1 + O(r).$$

Hence, the scaled system in polar form can be written as

$$\dot{r}r = -(1 - 2a_1)r \langle p_1, u \rangle + O(r^2),$$

$$r\dot{u} = \langle p_1, u \rangle u - p_1 + O(r).$$

We emphasize that the $$O(r)$$ and $$O(r^2)$$ terms are smooth functions of $$r, u$$ as long as $$|r| < \varepsilon$$. We consider the "semi-scaled" polar system

$$\dot{r} = -(1 - 2a_1)r \langle p_1, u \rangle + O(r^2),$$ (17a)

$$\dot{u} = \langle p_1, u \rangle u - p_1 + O(r),$$ (17b)

which has the same trajectories as the original system, just with different time parameterizations. The advantage of this modified system is that the equations are smooth on $$(-\varepsilon, \varepsilon) \times S^{d-1}$$.

Observe that the system (17) has $$\{0\} \times S^{d-1}$$ invariant and has a fixed point $$(r^*, u^*) = (0, p_1)$$. The fixed point $$p_1$$ is repelling on the invariant manifold $$\{0\} \times S^{d-1}$$; to see this, observe that

$$\langle p_1, u \rangle^{\cdot} = \langle p_1, u \rangle^2 - 1$$

when $$r = 0$$. In fact, if we assign the coordinate $$\theta$$ on any great circle joining $$p_1$$ and $$-p_1$$ on $$S^{d-1}$$ so that $$u = e^{i\theta}$$ and $$p_1 = 1$$, then the system reduces to $$\dot{\theta} = \sin \theta$$. We also see that the $$\dot{r}$$ equation linearized at $$(0, p_1)$$ is $$\dot{r} = -(1 - 2a_1)r$$; therefore, the linearization of (17) has the single negative eigenvalue $$-(1 - 2a_1)$$ and $$d - 1$$ positive eigenvalues $$+1$$. Therefore, $$(0, p_1)$$ is a saddle with a one-dimensional stable manifold and hence has a unique trajectory $$(r(t), u(t)) \to (0, p_1)$$ with $$r(t) > 0$$.

Now, suppose we have a trajectory $$w(t) \to p_1$$ in our original system (14). The corresponding trajectory for (17) will have $$r(t) \to 0$$; we cannot achieve $$r(t) = 0$$ in finite time because the manifold $$\{r = 0\}$$ is invariant for (17). We must prove that $$u(t) \to p_1$$ so that this trajectory is, in fact, the saddle stable manifold. Observe that

$$\langle p_1, u \rangle^{\cdot} = \langle p_1, u \rangle^2 - 1 + O(r).$$

Also, note that $$\langle p_1, u(t) \rangle > 0$$ since $$|w(t)| < 1$$. Let $$0 < c < 1$$; then, for some $$T \geq 0$$, $$t \geq T$$ implies $$O(r(t)) \leq (1 - c^2)/2$$. Now, suppose $$0 < \langle p_1, u(t_0) \rangle < c$$ for some $$t_0 \geq T$$; then, $$0 < \langle p_1, u(t) \rangle c$$ for all $$t \geq t_0$$. This is because the function $$t \mapsto \langle p_1, u(t) \rangle$$ is decreasing if $$0 < \langle p_1, u(t) \rangle < c$$,

$$\langle p_1, u(t) \rangle^{\cdot} \leq c^2 - 1 + \frac{1}{2}(1 - c^2) = -\frac{1}{2}(1 - c^2)$$

as long as $$0 < \langle p_1, u(t) \rangle < c$$. However, this also implies that eventually $$\langle p_1, u(t) \rangle < 0$$, which is a contradiction. Hence, we must have $$\langle p_1, u(t) \rangle \geq c$$ for all $$t \geq T$$, which proves that $$u(t) \to p_1$$. $$\square$$

## IX. SUMMARY AND DISCUSSION

The natural hyperbolic geometry on the unit ball, with isometries consisting of the higher-dimensional Möbius group, is key to understanding the dynamics of the Kuramoto model on a sphere. Using this framework, we see that dynamical trajectories of (1) are constrained to lie on group orbits, and we can explicitly give the equations for the reduced dynamics on the group orbits. For the special class of linear order parameters, the dynamics can be further reduced to a flow on the unit ball $$B^d$$, which is gradient with respect to the hyperbolic metric. We analyze this flow and prove a global synchronization result for the system (1) for linear order parameters with positive weights and no weight greater than half the total. This illustrates the power of the geometric/group-theoretic approach.

We conclude with some directions for future research. The case of linear order parameters with both positive and negative weights can, in principle, be explored using similar methods; it will also reduce to a hyperbolic gradient system on the ball $$B^d$$. In particular, the case when the sum of the weights is 0 should have some intriguing dynamics. For the original $$(d = 2)$$ Kuramoto model, the dynamics are Hamiltonian and equivalent to the vector field on the unit disk given by placing a collection of point charges on the unit circle, with total charge 0, as shown by Chen et al.$$^{27}$$ Of course, this result cannot generalize to all higher dimensions $$d$$; Hamiltonian dynamics is only possible in even dimensions.

Another possible direction to explore is the case of systems where the oscillator population is divided into two or more families with different intrinsic rotational terms $$A_i$$, and the coupling across families differs from the coupling within families. For the case $$d = 2$$, these systems often support "chimera states," in which one or more families synchronize while others tend to a partially disordered configuration. These states can be dynamically stable within their Möbius group orbits. Our framework enables the dynamical reduction of multi-family networks of higher-dimensional oscillators and makes possible the study of the dynamics of these networks without necessarily passing to the continuum limit, as is often done as a simplifying step in the analysis of Kuramoto networks.

## ACKNOWLEDGMENTS

This research was supported in part by the National Science Foundation (NSF) Research Training Group Grant: Dynamics, Probability, and PDEs in Pure and Applied Mathematics (Grant No. DMS-1645643) and by the NSF (Grant No. DMS-1910303). We thank Vladimir Jačimović and Max Lohe for helpful comments on a preprint of this paper.

## DATA AVAILABILITY

The data that support the findings of this study are available within the article.

## REFERENCES

$$^1$$Y. Kuramoto, "Self-entrainment of a population of coupled non-linear oscillators," in *International Symposium on Mathematical Problems in Theoretical Physics* (Springer, 1975), pp. 420–422.

$$^2$$Y. Kuramoto, *Chemical Oscillations, Waves, and Turbulence* (Springer, 1984).

Chaos 31, 093113 (2021); doi: 10.1063/5.0060233       31, 093113-13
Published under an exclusive license by AIP Publishing






Chaos    ARTICLE  scitation.org/journal/cha

<sup>3</sup>A. T. Winfree, "Biological rhythms and the behavior of populations of coupled oscillators," J. Theor. Biol. **16**, 15–42 (1967).

<sup>4</sup>A. T. Winfree, *The Geometry of Biological Time* (Springer, 1980).

<sup>5</sup>S. H. Strogatz, "From Kuramoto to Crawford: Exploring the onset of synchronization in populations of coupled oscillators," Phys. D **143**, 1–20 (2000).

<sup>6</sup>A. Pikovsky, M. Rosenblum, and J. Kurths, *Synchronization: A Universal Concept in Nonlinear Sciences* (Cambridge University Press, 2003), Vol. 12.

<sup>7</sup>S. Strogatz, *Sync* (Hyperion, 2003).

<sup>8</sup>J. A. Acebrón, L. L. Bonilla, C. J. P. Vicente, F. Ritort, and R. Spigler, "The Kuramoto model: A simple paradigm for synchronization phenomena," Rev. Mod. Phys. **77**, 137 (2005).

<sup>9</sup>F. Dörfler and F. Bullo, "Synchronization in complex networks of phase oscillators: A survey," Automatica **50**, 1539–1564 (2014).

<sup>10</sup>A. Pikovsky and M. Rosenblum, "Dynamics of globally coupled oscillators: Progress and perspectives," Chaos **25**, 097616 (2015).

<sup>11</sup>F. A. Rodrigues, T. K. D. Peron, P. Ji, and J. Kurths, "The Kuramoto model in complex networks," Phys. Rep. **610**, 1–98 (2016).

<sup>12</sup>C. Bick, M. Goodfellow, C. R. Laing, and E. A. Martens, "Understanding the dynamics of biological and neural oscillator networks through exact mean-field reductions: A review," J. Math. Neurosci. **10**, 9 (2020).

<sup>13</sup>K. Wiesenfeld, P. Colet, and S. H. Strogatz, "Synchronization transitions in a disordered Josephson series array," Phys. Rev. Lett. **76**, 404 (1996).

<sup>14</sup>K. Wiesenfeld, P. Colet, and S. H. Strogatz, "Frequency locking in Josephson arrays: Connection with the Kuramoto model," Phys. Rev. E **57**, 1563 (1998).

<sup>15</sup>K. Y. Tsang, R. E. Mirollo, S. H. Strogatz, and K. Wiesenfeld, "Dynamics of a globally coupled oscillator array," Phys. D **48**, 102–112 (1991).

<sup>16</sup>J. W. Swift, S. H. Strogatz, and K. Wiesenfeld, "Averaging of globally coupled oscillators," Phys. D **55**, 239–250 (1992).

<sup>17</sup>S. Nichols and K. Wiesenfeld, "Ubiquitous neutral stability of splay-phase states," Phys. Rev. A **45**, 8430 (1992).

<sup>18</sup>S. Watanabe and S. H. Strogatz, "Integrability of a globally coupled oscillator array," Phys. Rev. Lett. **70**, 2391 (1993).

<sup>19</sup>S. Watanabe and S. H. Strogatz, "Constants of motion for superconducting Josephson arrays," Phys. D **74**, 197–253 (1994).

<sup>20</sup>C. J. Goebel, "Comment on 'Constants of motion for superconductor arrays,'" Phys. D **80**, 18–20 (1995).

<sup>21</sup>E. Ott and T. M. Antonsen, "Low dimensional behavior of large systems of globally coupled oscillators," Chaos **18**, 037113 (2008).

<sup>22</sup>E. Ott and T. M. Antonsen, "Long time evolution of phase oscillator systems," Chaos **19**, 023117 (2009).

<sup>23</sup>A. Pikovsky and M. Rosenblum, "Partially integrable dynamics of hierarchical populations of coupled oscillators," Phys. Rev. Lett. **101**, 264103 (2008).

<sup>24</sup>S. A. Marvel, R. E. Mirollo, and S. H. Strogatz, "Identical phase oscillators with global sinusoidal coupling evolve by Möbius group action," Chaos **19**, 043104 (2009).

<sup>25</sup>I. Stewart, "Phase oscillators with sinusoidal coupling interpreted in terms of projective geometry," Int. J. Bifurcation Chaos **21**, 1795–1804 (2011).

<sup>26</sup>B. Chen, J. R. Engelbrecht, and R. Mirollo, "Hyperbolic geometry of Kuramoto oscillator networks," J. Phys. A: Math. Theor. **50**, 355101 (2017).

<sup>27</sup>B. Chen, J. R. Engelbrecht, and R. Mirollo, "Dynamics of the Kuramoto-Sakaguchi oscillator network with asymmetric order parameter," Chaos **29**, 013126 (2019).

<sup>28</sup>M. Lohe, "Non-Abelian Kuramoto models and synchronization," J. Phys. A: Math. Theor. **42**, 395101 (2009).

<sup>29</sup>M. Lohe, "Quantum synchronization over quantum networks," J. Phys. A: Math. Theor. **43**, 465301 (2010).

<sup>30</sup>T. Tanaka, "Solvable model of the collective motion of heterogeneous particles interacting on a sphere," New J. Phys. **16**, 023016 (2014).

<sup>31</sup>D. Chi, S.-H. Choi, and S.-Y. Ha, "Emergent behaviors of a holonomic particle system on a sphere," J. Math. Phys. **55**, 052703 (2014).

<sup>32</sup>S.-H. Choi and S.-Y. Ha, "Complete entrainment of Lohe oscillators under attractive and repulsive couplings," SIAM J. Appl. Dyn. Syst. **13**, 1417–1441 (2014).

<sup>33</sup>S.-Y. Ha, D. Ko, J. Park, and X. Zhang, "Collective synchronization of classical and quantum oscillators," EMS Surv. Math. Sci. **3**, 209–267 (2016).

<sup>34</sup>S.-Y. Ha, D. Ko, and S. W. Ryoo, "On the relaxation dynamics of Lohe oscillators on some Riemannian manifolds," J. Stat. Phys. **172**, 1427–1478 (2018).

<sup>35</sup>M. Lohe, "Higher-dimensional generalizations of the Watanabe–Strogatz transform for vector models of synchronization," J. Phys. A: Math. Theor. **51**, 225101 (2018).

<sup>36</sup>V. Jaćimović and A. Crnkić, "Low-dimensional dynamics in non-Abelian Kuramoto model on the 3-sphere," Chaos **28**, 083105 (2018).

<sup>37</sup>S. Chandra, M. Girvan, and E. Ott, "Continuous versus discontinuous transitions in the D-dimensional generalized Kuramoto model: Odd D is different," Phys. Rev. X **9**, 011002 (2019).

<sup>38</sup>S. Chandra, M. Girvan, and E. Ott, "Complexity reduction ansatz for systems of interacting orientable agents: Beyond the Kuramoto model," Chaos **29**, 053107 (2019).

<sup>39</sup>M. Lohe, "Systems of matrix Riccati equations, linear fractional transformations, partial integrability and synchronization," J. Math. Phys. **60**, 072701 (2019).

<sup>40</sup>L. DeVille, "Synchronization and stability for quantum Kuramoto," J. Stat. Phys. **174**, 160–187 (2019).

<sup>41</sup>S.-Y. Ha and H. Park, "From the Lohe tensor model to the Lohe Hermitian sphere model and emergent dynamics," SIAM J. Appl. Dyn. Syst. **19**, 1312–1342 (2020).

<sup>42</sup>M. Lohe, "On the double sphere model of synchronization," Phys. D **412**, 132642 (2020).

<sup>43</sup>S.-Y. Ha, D. Kim, H. Park, and S. W. Ryoo, "Constants of motion for the finite-dimensional Lohe type models with frustration and applications to emergent dynamics," Phys. D **416**, 132781 (2021).

<sup>44</sup>V. Jaćimović and A. Crnkić, "On reversibility of macroscopic and microscopic dynamics in the Kuramoto model," Phys. D **415**, 132762 (2021).

<sup>45</sup>X. Dai, K. Kovalenko, M. Molodyk, Z. Wang, X. Li, D. Musatov, A. Raigorodskii, K. Alfaro-Bittner, G. Cooper, G. Bianconi *et al.*, "D-dimensional oscillators in simplicial structures: Odd and even dimensions display different synchronization scenarios," Chaos, Solitons Fractals **146**, 110888 (2021).

<sup>46</sup>R. Olfati-Saber, "Swarms on sphere: A programmable swarm with synchronous behaviors like oscillator networks," in *Proceedings of the 45th IEEE Conference on Decision and Control* (IEEE, 2006), pp. 5060–5066.

<sup>47</sup>A. Beardon, *The Geometry of Discrete Groups* (Springer, 1983).

<sup>48</sup>M. Stoll, "Harmonic function theory on real hyperbolic space"; see https://citeseerx.ist.psu.edu/viewdoc/download?doi=10.1.1.561.4447&rep=rep1&type=pdf.

<sup>49</sup>W. Rudin, *Function Theory in the Unit Ball of* C<sup>n</sup> (Springer, 1980).

Chaos **31**, 093113 (2021); doi: 10.1063/5.0060233       **31**, 093113-14
Published under an exclusive license by AIP Publishing
