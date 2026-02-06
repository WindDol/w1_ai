

Available online at www.sciencedirect.com

ScienceDirect

IFAC PapersOnLine 55-13 (2022) 288–293

# Opinion Dynamics on the Sphere for Stable Consensus and Stable Bipartite Dissensus ★

Ziqiao Zhang ∗, Said Al-Abri ∗, Fumin Zhang ∗

∗ School of Electrical and Computer Engineering, Georgia Institute of Technology, Atlanta, GA 30332 USA
(e-mails: {ziqiao.zhang,saidalabri,fumin}@gatech.edu).

**Abstract:** In this paper, we develop a multi-agent model for opinion dynamics that offers rich behaviors of opinion formations. The states of the model are the opinions of the agents which are designed to evolve on a unit-sphere. The model is novel in that the input to the system is designed based on a correlation matrix of the opinions of neighboring agents. Interestingly, the model demonstrates both stable consensus as well as stable bipartite dissensus behaviors using an unsigned communication graph. This is different than existing results where stable dissensus can only be achieved via a signed graph. We present various stability results about the different equilibrium configurations. Additionally, we provide simulation results for a 2-dimensional system demonstrating the different resulting behaviors of the system.

Copyright © 2022 The Authors. This is an open access article under the CC BY-NC-ND license (https://creativecommons.org/licenses/by-nc-nd/4.0/)

**Keywords:** Social networks and Opinion dynamics; Multi-agent systems

## 1. INTRODUCTION

In social networks, individuals often exchange opinions with each other on certain matters and update their own opinions based on the shared information Proskurnikov and Tempo (2017, 2018). During this process of opinion formation, individuals will agree or disagree with others and they gradually reach a consensus or dissensus on their opinions. Researchers have been investigating models for opinion dynamics to understand the evolution and convergence of opinion formation Franci et al. (2021); Bizyaeva et al. (2021); Amelkin et al. (2017).

There are various types of opinion dynamics in the literature. For linear opinion dynamics, the opinion states evolve in the Euclidean space and are updated based on a weighted average of neighbors' opinions Olfati-Saber et al. (2007). Opinion dynamics have also been described on nonlinear manifolds such as the unit-sphere and the orthogonal group $SO(n)$, as in Sarlette and Sepulchre (2009), Sepulchre (2011) and Markdahl et al. (2017).

In this paper, we consider one kind of opinion dynamics where opinion states are modeled as unit-length vectors which correspond to points on the unit sphere. The dimension of each opinion state corresponds to the number of options or perspectives about a certain subject. The evolution of the state vector represents the change of opinions for each agent. Such evolution is affected by the opinions of neighboring agents.

The novelty of our proposed model is that we allow the influence from the neighboring agents to be modeled by a correlation matrix of the opinion states of the neighbors. This is different from existing work where the opinions from neighbors are (weighted) averaged Proskurnikov et al. (2015); Xia et al. (2016); Shi et al. (2019); Caponigro et al. (2015). Interestingly, our model accomplishes both stable consensus as well as stable bipartite dissensus behaviors using the same unsigned communication graph. This has not been observed in the literature. Most existing results Altafini (2013); Ma et al. (2018) report stable dissensus via a signed graph, while stable dissensus cannot be achieved by an average consensus algorithm on unsigned graphs Caponigro et al. (2015). In our recent work Zhang et al. (2021), we have shown that stable dissensus can be achieved under unsigned graphs while using a specially designed covariance matrix of the opinions from neighboring agents. However, this algorithm cannot attain a stable consensus using the same unsigned graph. This paper reports the first results for the bi-stability of the consensus and dissensus states on unsigned communication graphs.

Our model is inspired by the Oja PCA flow Oja (1982); Wei-Yong Yan et al. (1994); Yoshizawa et al. (2001), where unit vectors on a sphere converge to the eigenvectors of a constant matrix. In contrast, the flow dynamics in our formulation deal with a time-varying correlation matrix determined by the opinions of neighboring agents. The time-varying correlation matrix brings theoretical challenges in characterizing the equilibrium sets and analyzing their stability properties.

The main contributions in this paper are as follows. The first contribution is proposing novel modeling for opinion dynamics on the sphere using a correlation matrix formed by neighboring opinions. The second contribution is characterizing three equilibrium sets, which are consensus, bipartite dissensus, and orthogonal dissensus. The third contribution is the linearization-based stability results, and the construction of a Lyapunov function to estimate

★ The research work is supported by ONR grants N00014-19-1-2556 and N00014-19-1-2266; AFOSR grant FA9550-19-1-0283; NSF grants CNS-1828678, S&AS-1849228 and GCR-1934836; NRL grants N00173-17-1-G001 and N00173-19-P-1412; and NOAA grant NA16NOS0120028.

2405-8963 Copyright © 2022 The Authors. This is an open access article under the CC BY-NC-ND license.
Peer review under responsibility of International Federation of Automatic Control.
10.1016/j.ifacol.2022.07.274






Ziqiao Zhang et al. / IFAC PapersOnLine 55-13 (2022) 288–293    289

the region of attractions for the bi-stable equilibrium sets. Additionally, simulation results for both consensus and bipartite dissensus behaviors have been provided for a 40-agent opinion dynamics on the circle $\mathbb{S}^1$.

The rest of the paper is organized as follows. The problem formulation is given in Section 2. The characterization of different equilibrium sets is presented in Section 3. Local stability analysis and Lyapunov stability analysis for different types of equilibrium sets are given in Section 4 and Section 5, respectively. Lastly, simulation results are provided in Section 6 and concluding remarks are presented in Section 7.

## 2. PROBLEM FORMULATION

Consider a group of $N \geq 2$ agents exchanging opinions about given options. The opinion of any agent $i = 1, 2, ..., N$ is represented by a unit-length vector $v_i \in \mathbb{R}^d$, $\|v_i\|_2 = 1$. Each opinion state evolves on the surface of the unit sphere $\mathbb{S}^{d-1}$ according to the nonlinear dynamics (Caponigro et al. (2015); Markdahl et al. (2017))

$$\dot{v}_i = (I - v_i v_i^{\top}) u_i, \quad \forall i,$$ (1)

where $I \in \mathbb{R}^{d \times d}$ is the identity matrix and $u_i = u_i(v) \in \mathbb{R}^d$ is a control input for agent $i$, where $v = [v_1^{\top}, \cdots, v_N^{\top}]^{\top} \in \mathbb{R}^{Nd}$ is a vector containing the opinion states of all agents. The matrix $(I - v_i v_i^{\top})$ projects $u_i$ onto the tangent space of $v_i$ and hence $\dot{v}_i$ is always normal to $v_i$. This implies that the length of each vector is preserved and thus if for each agent $\|v_i(0)\|_2 = 1$, then all $v_i(t)$ will evolve on the unit-sphere $\mathbb{S}^{d-1}$ for all $t > 0$.

The interactions between agents are described by an unsigned graph $\mathcal{G} = (\mathcal{V}, \mathcal{E})$ where $\mathcal{V}$ is the set of all agents with cardinality $|\mathcal{V}| = N$ and $\mathcal{E}$ is the set of all edges.

**Assumption 1.** The graph $\mathcal{G} = (\mathcal{V}, \mathcal{E})$ formed by the group of agents is undirected and fully connected, i.e. $(i, j) \in \mathcal{E}$ and $(j, i) \in \mathcal{E}$ for any $i, j \in \mathcal{V}, i \neq j$.

**Assumption 2.** The graph $\mathcal{G} = (\mathcal{V}, \mathcal{E})$ formed by the group of agents is unweighted and unsigned, i.e., agents treat opinion states from other agents with equal weights, and there are no antagonistic interactions among the agents.

Define $M(v(t)) \triangleq \frac{1}{|\mathcal{V}|} \sum_{k \in \mathcal{V}} v_k(t) v_k(t)^{\top}$ as the correlation matrix. Note that $M(v(t)) \in \mathbb{R}^{d \times d}$ can be either a positive definite or positive semi-definite matrix. In this paper, all $v_i(t)$ are time-varying variables, and for simplicity, in what follows we will drop the time argument $t$.

Consider the control input for agent $i$ as

$$u_i = M(v) v_i = \frac{1}{|\mathcal{V}|} \sum_{k \in \mathcal{V}} \langle v_k, v_i \rangle v_k, \quad \forall i \in \mathcal{V},$$ (2)

where $\langle \cdot, \cdot \rangle$ represents the inner product. Substituting (2) into (1) leads to the closed loop opinion dynamics

$$\dot{v}_i = (I - v_i v_i^{\top}) M(v) v_i, \quad \forall i \in \mathcal{V}.$$ (3)

In this paper, we aim to study the behavior of the time-varying correlation-based opinion dynamics (3). The first goal is to characterize the equilibrium configurations of (3). The second goal is to derive theoretical conditions under which the system will pursue either a consensus or a dissensus behavior.

Different opinion behaviors in S² Consensus (b) Bipartite dissensus (c) Orthogonal dissensus

Fig. 1. Different opinion behaviors in $\mathbb{S}^2$.

## 3. CHARACTERIZATION OF EQUILIBRIUM SETS

In order to characterize the equilibrium configurations of the closed-loop opinion dynamics (3), we first introduce in this section definitions of several behaviors of opinion formations.

**Definition 3.** (Consensus Behavior). The opinion states are said to be in *consensus* if the states belong to the consensus set $\mathcal{C} \triangleq \{v = [v_1^{\top}, \cdots, v_N^{\top}]^{\top} \in \mathbb{R}^{Nd} | v_i = v_j, \|v_i\|_2 = \|v_j\|_2 = 1, \forall(i, j) \in \mathcal{E}\}$, where $\mathcal{E}$ is the set of all edges in the graph.

**Definition 4.** (Bipartite Dissensus Behavior). The opinion states are said to be in *bipartite dissensus* if they belong to the bipartite dissensus set $\mathcal{B} \triangleq \{v = [v_1^{\top}, \cdots, v_N^{\top}]^{\top} \in \mathbb{R}^{Nd} | v_i = \pm v_j, \|v_i\|_2 = \|v_j\|_2 = 1, \forall(i, j) \in \mathcal{E}\} \backslash \mathcal{C}$.

*Remark 5.* Note that the notion of "bipartite dissensus" was introduced as "bipartite consensus" in Altafini (2013). We changed the notion for the convenience of this paper.

**Definition 6.** (Orthogonal Dissensus Behavior). The opinion states are said to be in *orthogonal dissensus* if they belong to the orthogonal dissensus set $\mathcal{O} \triangleq \{v = [v_1^{\top}, \cdots, v_N^{\top}]^{\top} \in \mathbb{R}^{Nd} | \langle v_i, v_j \rangle \in \{-1, 0, 1\}, \|v_i\|_2 = \|v_j\|_2 = 1, \forall(i, j) \in \mathcal{E}\} \backslash (\mathcal{C} \cup \mathcal{B})$.

*Remark 7.* Bipartite dissensus and orthogonal dissensus behaviors are two special types of dissensus behavior.

**Lemma 8.** The consensus, bipartite dissensus, and orthogonal dissensus behaviors correspond to different equilibrium sets of the opinion dynamics (3).

**Proof.** If the opinion states are in consensus, then $v_i = v_c^*$ for all $i \in \mathcal{V}$ where $v_c^*$ is the consensus value. Hence, $M(v) \triangleq \frac{1}{|\mathcal{V}|} \sum_{k \in \mathcal{V}} v_k v_k^{\top} = v_c^* v_c^{*\top}$. This implies that $\dot{v}_i = (I - v_i v_i^{\top}) M(v) v_i = (I - v_c^* v_c^{*\top}) v_c^* v_c^{*\top} v_c^* = 0$ for all $i \in \mathcal{V}$. Hence, the consensus behavior is an equilibrium configuration.

If the opinion states are in bipartite dissensus, then $v_i = v_b^*$ for all $i \in \mathcal{V}_1$ and $v_i = -v_b^*$ for all $i \in \mathcal{V}_2$ where $v_b^*$ is one bipartite dissensus value. Hence, $M(v) \triangleq \frac{1}{|\mathcal{V}|} \sum_{k \in \mathcal{V}} v_k v_k^{\top} = v_b^* v_b^{*\top}$. This implies that $\dot{v}_i = (I - v_i v_i^{\top}) M(v) v_i = (I - v_b^* v_b^{*\top}) v_b^* v_b^{*\top} (\pm v_b^*) = 0$ for all $i \in \mathcal{V}$. Hence, the bipartite dissensus behavior is an equilibrium configuration.

If the opinion states are in orthogonal dissensus, then there exist $S$ non-empty sets $\mathcal{V}_1, \cdots, \mathcal{V}_S$ and $v_i = \pm v_s^*$ for $i \in \mathcal{V}_s$ where $v_s^*$ is one orthogonal value and $v_{s_1}^* \perp v_{s_2}^*$ for all $s_1 \neq s_2$. Hence, $M(v) \triangleq \frac{1}{|\mathcal{V}|} \sum_{k \in \mathcal{V}} v_k v_k^{\top} = \frac{1}{|\mathcal{V}|} \sum_{s=1}^S |\mathcal{V}_s| v_s^* v_s^{*\top}$. This implies that if $i \in \mathcal{V}_s$, then $v_i^{\top} v_{s'}^* = 0$ for all $s' \neq s$, and $\dot{v}_i = (I - v_i v_i^{\top}) M(v) v_i = (I - v_i v_i^{\top}) \frac{1}{|\mathcal{V}|} \sum_{s=1}^S |\mathcal{V}_s| v_s^* v_s^{*\top} v_i = (I -$
v_s^* v_s^{*\top}) \frac{1}{|V_s|} \sum_{v_s^* v_s^{*\top}(\pm v_s^*) = 0$$ for any $s = 1, \cdots, S$. Hence, the orthogonal dissensus behavior is an equilibrium configuration. ■

## 4. LOCAL STABILITY ANALYSIS

In this section, we use the linearization method to obtain local stability results for three different types of equilibrium sets (consensus, bipartite dissensus, and orthogonal dissensus).

### 4.1 Linearization of the Dynamics

Let $\dot{v}_i \triangleq f_i(v)$ where the function $f_i(v) : \mathbb{R}^{Nd} \to \mathbb{R}^d$ is as defined in (3) and $v = [v_1^\top, \cdots, v_N^\top]^\top \in \mathbb{R}^{Nd}$. Define the block-diagonal matrix $M' = \text{diag}([M, \cdots M]) \in \mathbb{R}^{Nd \times Nd}$. On the other hand, define the block-diagonal projection matrix $P \triangleq \text{diag}([P_1, \cdots P_N]) \in \mathbb{R}^{Nd \times Nd}$ where $P_i \triangleq I - v_i v_i^\top$. Then, we can write $\dot{v} = PM'v \triangleq f(v)$, where the matrix $P$ projects the vector field $M'v$ on to the tangent space of $(S^{d-1})^N$.

**Lemma 9.** The Jacobian matrix of the system $\dot{v} = f(v)$ is given by $\frac{\partial f}{\partial v} = \left[\frac{\partial f_i}{\partial v_k}\right]_{\forall i,k} \in \mathbb{R}^{Nd \times Nd}$, in which the diagonal terms, for all $i \in V$, are given by

$$\frac{\partial f_i}{\partial v_i} = \frac{1}{N} v_i v_i^\top + (\frac{1}{N} - v_i^\top M v_i)I + (I - 2v_i v_i^\top)M \quad (4)$$

where $M = M(v) = (1/N) \sum_{j=1}^N v_j v_j^\top$, and the off-diagonal terms, for all $i, k \in V, k \neq i$, are given by

$$\frac{\partial f_i}{\partial v_k} = \frac{1}{N}(v_k v_i^\top + v_i^\top v_i(I - 2v_i v_i^\top)). \quad (5)$$

**Proof.** The diagonal terms are given by

$$\frac{\partial f_i}{\partial v_i} = \frac{\partial(M v_i)}{\partial v_i} - \frac{\partial(v_i v_i^\top M v_i)}{\partial v_i}. \quad (6)$$

Taking the partial derivative of $M v_i$ we obtain

$$\frac{\partial(M v_i)}{\partial v_i} = \frac{1}{N}(\sum_{j \neq i} v_j v_j^\top + I) = M + \frac{1}{N}(I - v_i v_i^\top). \quad (7)$$

Taking partial derivative of $(v_i v_i^\top M v_i)$ we have

$$\frac{\partial(v_i v_i^\top M v_i)}{\partial v_i} = v_i^\top M v_i I + 2v_i v_i^\top M - 2v_i v_i^\top/N. \quad (8)$$

Substituting (7) and (8) into (6) leads to the claimed equation (4). The off-diagonal terms are

$$\frac{\partial f_i}{\partial v_k} = (I - v_i v_i^\top) \frac{\partial M v_i}{\partial v_k}. \quad (9)$$

However,

$$\frac{\partial M v_i}{\partial v_k} = \frac{\partial}{\partial v_k} \frac{1}{N} \sum_{j=1}^N v_j v_j^\top v_i = \frac{1}{N}(v_k v_i^\top + v_i^\top v_i I). \quad (10)$$

Substituting (10) into (9) leads to the claimed equation (5). ■

### 4.2 Local Stability for Equilibrium Sets

In this part, we apply the general linearization results in (4) and (5) to three special types of equilibria, consensus, bipartite dissensus, and orthogonal dissensus. Stability analysis based on eigenvalues will be provided for the three cases respectively.

**Lemma 10.** The consensus equilibrium set $\mathcal{C}$ is locally attractive.

**Proof.** Evaluating (4) and (5) at the consensus equilibrium leads to

$$\frac{\partial f_i}{\partial v_i} = -\frac{N-1}{N}(v_c^* v_c^{*\top} + I), \forall i \in V, \quad (11)$$

and

$$\frac{\partial f_i}{\partial v_k} = \frac{1}{N}(I - v_c^* v_c^{*\top}), \forall i, k \in V, k \neq i. \quad (12)$$

Then the Jacobian matrix $\frac{\partial f}{\partial v}$ has 3 different types of eigenvalues $\lambda_1 = 0, \lambda_2 = -\frac{2N-2}{N}$ and $\lambda_3 = -1$, with algebraic multiplicity of $d-1, N$, and $Nd - N - d + 1$ respectively. The derivation of negative eigenvalues for the Jacobian matrix at the consensus equilibrium is provided in Appendix. A.

We can find $d-1$ linearly independent vectors perpendicular to $v_c^*$, denoting by $w^l$ such that $w^l \perp v_c^*$ for all $l = 1, \cdots, d-1$. We can construct $d-1$ linearly independent vectors $x^l = [w^{l\top}, w^{l\top}, \cdots, w^{l\top}]^\top \in \mathbb{R}^{Nd}$ $l = 1, \cdots, d-1$ such that

$$\frac{\partial f}{\partial v} x^l = \left[(\sum_{k \neq i} \frac{\partial f_i}{\partial v_k} w^l) + \frac{\partial f_i}{\partial v_i} w^l\right]_{\forall i}$$
$$= \left[(\sum_{k \neq i} \frac{1}{N}(I - v_c^* v_c^{*\top})w^l) - \frac{N-1}{N}(v_c^* v_c^{*\top} + I)w^l\right]_{\forall i}$$
$$= \left[\frac{N-1}{N} w^l - \frac{N-1}{N} w^l\right]_{\forall i} = 0. \quad (13)$$

Thus, $x^1, \cdots, x^{d-1}$ are the $d-1$ linearly independent eigenvectors corresponding to $\lambda_1 = 0$. These eigenvectors are all perpendicular to the consensus $[v_c^{*\top}, v_c^{*\top}, \cdots, v_c^{*\top}]^\top$. This corresponds to the constraint that all opinion states are unit vectors on the sphere. Meanwhile, local perturbations along these eigenvectors $x^l$ will not disturb the consensus state. which means that the eigenvalue $\lambda_1 = 0$ will not affect the stability for consensus set $\mathcal{C}$.

Since the other eigenvalues are all negative, then the consensus set $\mathcal{C}$ is attractive. ■

**Remark 11.** Set $\mathcal{C}$ is composed by all consensus equilibria and has dimension $d-1$, which is the same as the algebraic multiplicity of $\lambda_1 = 0$ for the Jacobian matrix $\frac{\partial f}{\partial v}$. If all consensus opinion states are perturbed locally in the same orthogonal direction, then the opinion states will stay in consensus and move to a new consensus state in $\mathcal{C}$ together.

**Lemma 12.** The bipartite dissensus set $\mathcal{B}$ is locally attractive.

**Proof.** Evaluating (4) and (5) at the bipartite dissensus equilibrium leads to

$$\frac{\partial f_i}{\partial v_i} = -\frac{N-1}{N}(v_b^* v_b^{*\top} + I), \forall i \in V, \quad (14)$$

and

$$\frac{\partial f_i}{\partial v_k} = \frac{a_{ik}}{N}(I - v_b^* v_b^{*\top}), \forall i, k \in V, k \neq i, \quad (15)$$

where $a_{ik} \in \{\pm 1\}$ is defined as $a_{ik} = 1$ if $v_k = v_i$ and $a_{ik} = -1$ if $v_k = -v_i$. According to the definition of $a_{ik}$, we can have $a_{ii} = 1, a_{ij} = a_{ji}$ and $a_{ij} = a_{ik}a_{kj}$ for all $i, j, k$.

Then the Jacobian matrix $\frac{\partial f}{\partial v}$ has 3 different eigenvalues $\lambda_1 = 0, \lambda_2 = -\frac{2N-2}{N}, \lambda_3 = -1$ with algebraic multiplicity of $d-1, N, Nd - N - d + 1$ respectively. The derivation





Ziqiao Zhang et al. / IFAC PapersOnLine 55-13 (2022) 288–293    291

of negative eigenvalues for the Jacobian matrix at the bipartite dissensus equilibrium is almost the same as that for consensus equilibrium. We will not provide details for this case.

We can find $d - 1$ linearly independent vectors perpendicular to $\boldsymbol{v}_b^*$, denoting by $\boldsymbol{y}^l$ such that $\boldsymbol{y}^l \perp \boldsymbol{v}_b^*$ for all $l = 1, \cdots, d - 1$. We can construct $d - 1$ linearly independent vectors $\boldsymbol{x}^l = [a_{11}\boldsymbol{y}^{l\top}, a_{12}\boldsymbol{y}^{l\top}, \cdots, a_{1N}\boldsymbol{y}^{l\top}]^\top \in \mathbb{R}^{Nd}$ $l = 1, \cdots, d - 1$ such that

$$\frac{\partial \boldsymbol{f}}{\partial \boldsymbol{v}} \boldsymbol{x}^l = \left[\left(\sum_{k \neq i} \frac{\partial \boldsymbol{f}}{\partial \boldsymbol{v}_i} a_{1k}\boldsymbol{y}^l\right) + \frac{\partial \boldsymbol{f}}{\partial \boldsymbol{v}_i} a_{1i}\boldsymbol{y}^l\right]_{\forall i}$$

$$= \left[a_{1i}\left(\sum_{k \neq i} \frac{\partial \boldsymbol{f}}{\partial \boldsymbol{v}_i} a_{1k}\boldsymbol{y}^l\right) + a_{1i} \frac{\partial \boldsymbol{f}}{\partial \boldsymbol{v}_i} \boldsymbol{y}^l\right]_{\forall i}$$

$$= \left[a_{1i}\left(\left(\sum_{k \neq i} \frac{1}{N}(\boldsymbol{I} - \boldsymbol{v}_b^*\boldsymbol{v}_b^{*\top})\boldsymbol{y}^l\right) - \frac{N-1}{N}(\boldsymbol{v}_b^*\boldsymbol{v}_b^{*\top} + \boldsymbol{I})\boldsymbol{y}^l\right)\right]_{\forall i}$$

$$= \left[a_{1i} \frac{N-1}{N} \boldsymbol{y}^l - a_{1i} \frac{N-1}{N} \boldsymbol{y}^l\right]_{\forall i} = \boldsymbol{0}.$$
(16)

Thus, $\boldsymbol{x}^1, \cdots, \boldsymbol{x}^{d-1}$ are the $d - 1$ linearly independent eigenvectors corresponding to $\lambda_1 = 0$. These eigenvectors are all perpendicular to the bipartite dissensus $[\pm\boldsymbol{v}_b^{*\top}, \pm\boldsymbol{v}_b^{*\top}, \cdots, \pm\boldsymbol{v}_b^{*\top}]$. This corresponds to the constraint that all opinion states are unit vectors on the sphere. Meanwhile, local perturbation along these eigenvectors $\boldsymbol{x}^l$ will not disturb the bipartite dissensus state, which means that the eigenvalue $\lambda_1 = 0$ will not affect the stability for the bipartite dissensus set $\mathcal{B}$.

Since the other eigenvalues are all negative, then the bipartite dissensus set $\mathcal{B}$ is stable. ■

*Remark 13.* Set $\mathcal{B}$ is composed by all bipartite dissensus equilibria and has dimension $d-1$, which is the same as the algebraic multiplicity of $\lambda_1 = 0$ for the Jacobian matrix $\frac{\partial \boldsymbol{f}}{\partial \boldsymbol{v}}$. If opinion states from same group are perturbed locally in the same orthogonal direction while opinion states from different groups are perturbed locally in the opposite orthogonal direction, then the opinion states will stay in the bipartite dissensus and move to a new bipartite dissensus state in $\mathcal{B}$.

*Lemma 14.* Suppose all opinion states are in orthogonal dissensus. Then the orthogonal dissensus equilibrium is locally repulsive.

**Proof.** The diagonal terms of the Jacobian matrix $\frac{\partial \boldsymbol{f}}{\partial \boldsymbol{v}}$ for all $i \in \mathcal{V}$ are give by (4). For off-diagonal terms, if $\boldsymbol{v}_k \perp \boldsymbol{v}_i$ then $\frac{\partial \boldsymbol{f}}{\partial \boldsymbol{v}_i} = \frac{1}{N} \boldsymbol{v}_k \boldsymbol{v}_i^\top$, and if $\boldsymbol{v}_k = \pm\boldsymbol{v}_i$ then $\frac{\partial \boldsymbol{f}}{\partial \boldsymbol{v}_i} = \frac{1}{N} \boldsymbol{v}_k^\top \boldsymbol{v}_i (\boldsymbol{I} - \boldsymbol{v}_i \boldsymbol{v}_i^\top)$. Consider the orthogonal dissensus where the set of nodes are partitioned by $S$ non-empty sets $(2 \leq S \leq d)$ $\mathcal{V}_1, \cdots, \mathcal{V}_S$ and $\boldsymbol{v}_i = \pm\boldsymbol{v}_{s}^*$ for $i \in \mathcal{V}_s$ where $\boldsymbol{v}_{s}^*$ is one orthogonal value and $\boldsymbol{v}_{s_1}^* \perp \boldsymbol{v}_{s_2}^*$ for all $s_1 \neq s_2$. Define $\lambda_s \triangleq \boldsymbol{v}_s^{*\top} \boldsymbol{M} \boldsymbol{v}_s^* \in [\frac{1}{N}, 1]$. We can choose two different node sets $\mathcal{V}_{s_1}, \mathcal{V}_{s_2}$. Then the Jacobian matrix $\frac{\partial \boldsymbol{f}}{\partial \boldsymbol{v}}$ has one positive eigenvector $(\lambda_{s_1} + \lambda_{s_2})$. More details on the derivation of eigenvalue can be found in Appendix B.

Therefore, the orthogonal dissensus equilibrium is locally repulsive. ■

## 5. LYAPUNOV STABILITY ANALYSIS

In this section, we use Lyapunov techniques to study the stability of the considered equilibrium configurations and estimate the regions of attraction.

### 5.1 Stability of the Consensus Configuration

*Theorem 15.* Consider the opinion dynamics (3), suppose at time $t = 0$, $\langle \boldsymbol{v}_i(0), \boldsymbol{v}_j(0) \rangle \in (0, 1], \forall(i, j) \in \mathcal{E}$. Then the consensus set $\mathcal{C}$ is asymptotically stable.

**Proof.** Define the set $\Omega_1 = \{\boldsymbol{v} | \langle \boldsymbol{v}_i, \boldsymbol{v}_j \rangle \in (0, 1], \|\boldsymbol{v}_i\|_2 = \|\boldsymbol{v}_j\|_2 = 1, \forall(i, j) \in \mathcal{E}\}$. Define the function

$$W = \frac{1}{2} \sum_{(i,j) \in \mathcal{E}} (1 - \langle \boldsymbol{v}_i, \boldsymbol{v}_j \rangle^2),$$
(17)

where $W \geq 0$ and $W = 0$ if and only if $\boldsymbol{v}_i = \boldsymbol{v}_j$ for all $\boldsymbol{v}_i, \boldsymbol{v}_j \in \Omega_1$. Let $\boldsymbol{X}_i = \boldsymbol{v}_i \boldsymbol{v}_i^\top$. Then, we obtain

$$\dot{W} = - \sum_{(i,j) \in \mathcal{E}} \langle \bold symbol{v}_i, \boldsymbol{v}_j \rangle [\langle \boldsymbol{v}_i, (\boldsymbol{I} - \boldsymbol{X}_j) \boldsymbol{u}_j \rangle + \langle \boldsymbol{v}_j, (\boldsymbol{I} - \boldsymbol{X}_i) \boldsymbol{u}_i \rangle]$$

$$= - \sum_{i \in \mathcal{V}} \sum_{j \in \mathcal{V}} \langle \boldsymbol{v}_i, \boldsymbol{v}_j \rangle \langle \boldsymbol{v}_j, (\boldsymbol{I} - \boldsymbol{X}_i) \boldsymbol{u}_i \rangle$$

$$= -N \sum_{i \in \mathcal{V}} \langle \boldsymbol{u}_i, (\boldsymbol{I} - \boldsymbol{X}_i) \boldsymbol{u}_i \rangle$$

$$= N \sum_{i \in \mathcal{V}} [\langle \boldsymbol{u}_i, \boldsymbol{v}_i \rangle^2 - \|\boldsymbol{u}_i\|_2^2] \leq 0,$$
(18)

where, in $\Omega_1$, $\dot{W} = 0$ if and only if $\boldsymbol{v}_i = \boldsymbol{v}_j$, for all $i, j$. Define

$$Q = \frac{1}{2} \sum_{(i,j) \in \mathcal{E}} \langle \boldsymbol{v}_i, \boldsymbol{v}_j \rangle^2.$$
(19)

In virtue of (17) and (18), we can see that $\dot{Q} = -\dot{W}$. Consider the Lyapunov candidate function

$$V_1 = \frac{W}{Q},$$
(20)

where $V_1 \geq 0$, and, in $\Omega_1$, $V_1 = 0$ if and only if $\boldsymbol{v}_i = \boldsymbol{v}_j$, for all $i, j$. Additionally, $V \to \infty$ if $\langle \boldsymbol{v}_i, \boldsymbol{v}_j \rangle \to 0$ for any $i, j$. Then we obtain

$$\dot{V}_1 = \frac{(Q + W)}{Q^2} \dot{W} \leq 0,$$
(21)

where $\dot{V}_1 = 0$ if and only if $\boldsymbol{v}_i = \boldsymbol{v}_i$, for all $i, j$. Additionally, $\dot{V}_1 \to -\infty$ if $\langle \boldsymbol{v}_i, \boldsymbol{v}_j \rangle \to 0$ for any $i, j$. This along the fact that $V_1 \to \infty$ whenever any $\langle \boldsymbol{v}_i, \boldsymbol{v}_j \rangle \to 0$ implies that $\Omega_1$ is forward invariant and thus the consensus equilibrium is asymptotically stable. ■

*Remark 16.* Since the matrix $\boldsymbol{M}(\boldsymbol{v})$ in (3) is time-varying, the existing constant-matrix convergence analysis of the Oja PCA flow in Yoshizawa et al. (2001) does not hold for our problem. Under the initial conditions described in *Theorem 15*, the states converge to a stable consensus equilibrium. This is a major distinction from the Oja PCA flow where the states converge to the principal eigenvectors of the underlying constant matrix.

### 5.2 Stability of the Bipartite Dissensus Configuration

Let us partition the edge set $\mathcal{E}$ into two sets: $\mathcal{E}_1$ and $\mathcal{E}_2$ such that $\mathcal{E}_1 \cap \mathcal{E}_2 = \emptyset, \mathcal{E}_1 \cup \mathcal{E}_2 = \mathcal{E}$. Then, for the bipartite dissensus we have the following result:

*Theorem 17.* Consider the opinion dynamics (3), suppose at time $t = 0$, $\langle \boldsymbol{v}_i(0), \boldsymbol{v}_j(0) \rangle \in (0, 1], \forall(i, j) \in \mathcal{E}_1$ and $\langle \boldsymbol{v}_i(0), \boldsymbol{v}_j(0) \rangle \in [-1, 0), \forall(i, j) \in \mathcal{E}_2$. Then the bipartite dissensus set $\mathcal{B}$ is asymptotically stable.





292    Ziqiao Zhang et al. / IFAC PapersOnLine 55-13 (2022) 288–293

**Proof.** Define the set $\Omega_2 = \{v | \langle v_i, v_j \rangle \in (0, 1] \forall (i, j) \in \mathcal{E}_1, \langle v_i, v_j \rangle \in [-1, 0) \forall (i, j) \in \mathcal{E}_2, \|v_i\|_2 = \|v_j\|_2 = 1 \forall (i, j) \in \mathcal{E}\}$. That is, the bipartite dissensus is the only equilibrium in $\Omega_2$. Then the same Lyapunov function (20) can be used to show that the set $\Omega_2$ is forward invariant. In particular, we can show that, in $\Omega_2$, it holds that $\dot{V} \leq 0$, where $\dot{V} = 0$ if and only if $v_i = v_j$ for all $(i, j) \in \mathcal{E}_1$, and $v_i = -v_j$ for all $(i, j) \in \mathcal{E}_2$. Additionally, $\dot{V} \to -\infty$ if $\langle v_i, v_j \rangle \to 0$ for any $i, j$. This along the fact that $V \to \infty$ whenever any $\langle v_i, v_j \rangle \to 0$ implies that $\Omega_2$ is forward invariant and thus the bipartite dissensus equilibrium is asymptotically stable. ■

### 5.3 Instability of the Orthogonal Dissensus Configuration

**Theorem 18.** Under the opinion dynamics defined in (3), the orthogonal set $\mathcal{O}$ is unstable.

**Proof.** Suppose the opinion states at orthogonal equilibrium can be divided into $S$ groups $\mathcal{V}_1, \cdots, \mathcal{V}_S$ such that $v_i = \pm v_s^*$ for $i \in \mathcal{V}_s$ where $v_s^*$ is one orthogonal value and $v_{s_1}^* \perp v_{s_2}^*$ for all $s_1 \neq s_2$. Hence, $M(v^*) = \frac{1}{|\mathcal{V}|} \sum_{s=1}^S |\mathcal{V}_s| v_s^* v_s^{*\top}$. Define $\gamma_{ij} = \langle v_i, v_j \rangle^2$ for $i \in \mathcal{V}_s$ and $j \in \mathcal{V} \backslash \mathcal{V}_s$ for all $s = 1, \cdots, S$, where $\gamma_{ij} \in [0, 1]$ and $\gamma_{ij} = 0$ if and only if $\langle v_i, v_j \rangle = 0$. Define an edge set $\mathcal{E}^o$ such that $(i, j) \in \mathcal{E}^o$ if $i \in \mathcal{V}_{s_1}, j \in \mathcal{V}_{s_2}$ and $s_1 \neq s_2$. Define $\gamma \in \mathbb{R}^{|\mathcal{E}^o|}$ to be the vector that contains all $\gamma_{ij}$.

Consider the Lyapunov candidate

$$V_2 = \frac{1}{2} \sum_s \sum_{i \in \mathcal{V}_s} \sum_{j \in \mathcal{V} \backslash \mathcal{V}_s} \gamma_{ij} = \frac{1}{2} \sum_s \sum_{i \in \mathcal{V}_s} \sum_{j \in \mathcal{V} \backslash \mathcal{V}_s} \langle v_i, v_j \rangle^2,$$ (22)

where $V_2 \geq 0$ and $V_2 = 0$ if and only if $\gamma_{ij} = 0$ i.e. $\langle v_i, v_j \rangle = 0$ for $i \in \mathcal{V}_{s_1}, j \in \mathcal{V}_{s_2}$ and $s_1 \neq s_2$. Then

$$\dot{V}_2 = \sum_s \sum_{i \in \mathcal{V}_s} \sum_{j \in \mathcal{V} \backslash \mathcal{V}_s} [\langle v_i, v_j \rangle \langle v_i, (I - V_j) u_j \rangle + \langle v_i, v_j \rangle \langle v_j, (I - V_i) u_i \rangle]$$

$$= 2 \sum_s \sum_{i \in \mathcal{V}_s} \sum_{j \in \mathcal{V} \backslash \mathcal{V}_s} \langle v_i, v_j \rangle \langle v_j, (I - V_i) u_i \rangle$$

$$= 2 \sum_s \sum_{i \in \mathcal{V}_s} \sum_{j \in \mathcal{V}} \langle v_i, v_j \rangle \langle v_j, (I - V_i) u_i \rangle$$ (23)

$$= 2N \sum_{i \in \mathcal{V}} [\|u_i\|_2^2 - \langle u_i, v_i \rangle^2] \geq 0.$$

Define the set $U = \{\gamma \in B | V_2 > 0\}$ where $B = \{\gamma \in \mathbb{R}^{|\mathcal{E}^o|} | \|\gamma\|_2 \leq 2|\mathcal{E}^o|\}$. Note that the nonempty set $U$ is contained in $B$. This implies that $\dot{V}_2 > 0$ for all points in $U$. Therefore, all the conditions in Theorem 4.3 in Khalil (2002) are met, and hence the equilibrium $\gamma = \mathbf{0}$, or the orthogonal dissensus equilibrium is unstable. ■

## 6. SIMULATION RESULTS

In this section, we simulate (3) using 40−agent system. We consider two types of initial conditions: (1) The opinion states satisfy $\langle v_i(0), v_j(0) \rangle > 0$ for all $i, j \in \mathcal{V}$; (2) The opinion states satisfy that there exist some $i, j \in \mathcal{V}$ such that $\langle v_i(0), v_j(0) \rangle < 0$.

As shown in Fig. 2, when the initial conditions satisfy $\langle v_i(0), v_j(0) \rangle > 0$ for all $i, j \in \mathcal{V}$, then the opinion states

![Three circles showing evolution at t=0s, t=0.05s, and t=0.15s with red dots representing opinion states converging to consensus]

Fig. 2. Evolution of opinion states towards consensus for a 40-agent system in $\mathbb{S}^1$ when $\langle v_i(0), v_j(0) \rangle > 0$ for all $i, j$. The red dots represent the opinion states.

![Three circles showing evolution at t=0s, t=0.2s, and t=0.3s with red dots representing opinion states forming two groups]

Fig. 3. Evolution of opinion states towards bipartite dissensus for a 40-agent system in $\mathbb{S}^1$ when $\langle v_i(0), v_j(0) \rangle < 0$ for some $i, j$. The red dots represent the opinion states.

are converging to the consensus configuration. Driven by the time-varying correlation-based opinion dynamics described in (3), the geodesics between opinion states gradually decrease until the opinion states settle on the consensus value.

For the example shown in Fig. 3, the initial opinion states are chosen such that there exist some $i_1, i_2, j_1, j_2$ where $\langle v_{i_1}(0), v_{j_1}(0) \rangle < 0$ and $\langle v_{i_2}(0), v_{j_2}(0) \rangle > 0$. Then, under the opinion dynamics (3), the opinion states are gradually forming two groups which become distinguishable at around time $t = 0.2$s. The opinion states within each group converge to a consensus, while the consensus values from the two groups are opposite to each other, leading to the bipartite dissensus behavior of the entire 40−agent system.

## 7. CONCLUSION

In this paper, we develop novel modeling of opinion dynamics on the sphere using a time-varying correlation matrix. Our stability analysis reveals that stable consensus and stable bipartite dissensus behaviors can be reached with an unsigned graph. Since in this paper we restrict the analysis on complete graphs, in the future we will consider incomplete graphs to study the role of the graph on shaping the system equilibrium configurations as well as convergence behaviors.

## REFERENCES

Altafini, C. (2013). Consensus problems on networks with antagonistic interactions. IEEE Transactions on Automatic Control, 58(4), 935–946.

Amelkin, V., Bullo, F., and Singh, A.K. (2017). Polar opinion dynamics in social networks. IEEE Transactions on Automatic Control, 62(11), 5650–5665.

Bizyaeva, A., Matthews, A., Franci, A., and Leonard, N.E. (2021). Patterns of nonlinear opinion formation on





Ziqiao Zhang et al. / IFAC PapersOnLine 55-13 (2022) 288–293    293

networks. In 2021 American Control Conference (ACC), 2739–2744. IEEE.

Caponigro, M., Lai, A.C., and Piccoli, B. (2015). A nonlinear model of opinion formation on the sphere. Discrete & Continuous Dynamical Systems-A, 35(9), 4241.

Franci, A., Bizyaeva, A., Park, S., and Leonard, N.E. (2021). Analysis and control of agreement and disagreement opinion cascades. Swarm Intelligence, 15(1), 47–82.

Khalil, H.K. (2002). Nonlinear systems.

Ma, H., Liu, D., Wang, D., Yang, X., and Li, H. (2018). Distributed algorithm for dissensus of a class of networked multiagent systems using output information. Soft Computing, 22(1), 273–282.

Markdahl, J., Thunberg, J., and Gonçalves, J. (2017). Almost global consensus on the n-sphere. IEEE Transactions on Automatic Control, 63(6), 1664–1675.

Oja, E. (1982). Simplified neuron model as a principal component analyzer. Journal of mathematical biology, 15(3), 267–273.

Olfati-Saber, R., Fax, J.A., and Murray, R.M. (2007). Consensus and cooperation in networked multi-agent systems. Proceedings of the IEEE, 95(1), 215–233.

Proskurnikov, A.V., Matveev, A.S., and Cao, M. (2015). Opinion dynamics in social networks with hostile camps: Consensus vs. polarization. IEEE Transactions on Automatic Control, 61(6), 1524–1536.

Proskurnikov, A.V. and Tempo, R. (2017). A tutorial on modeling and analysis of dynamic social networks. part i. Annual Reviews in Control, 43, 65–79.

Proskurnikov, A.V. and Tempo, R. (2018). A tutorial on modeling and analysis of dynamic social networks. part ii. Annual Reviews in Control, 45, 166–190.

Sarlette, A. and Sepulchre, R. (2009). Consensus optimization on manifolds. SIAM Journal on Control and Optimization, 48(1), 56–76.

Sepulchre, R. (2011). Consensus on nonlinear spaces. Annual reviews in control, 35(1), 56–64.

Shi, G., Altafini, C., and Baras, J.S. (2019). Dynamics over signed networks. SIAM Review, 61(2), 229–257.

Wei-Yong Yan, Helmke, U., and Moore, J.B. (1994). Global analysis of oja's flow for neural networks. IEEE Transactions on Neural Networks, 5(5), 674–683.

Xia, W., Cao, M., and Johansson, K.H. (2016). Structural balance and opinion separation in trust–mistrust social networks. IEEE Transactions on Control of Network Systems, 3(1), 46–56.

Yoshizawa, S., Helmke, U., and Starkov, K. (2001). Convergence analysis for principal component flows. International Journal of Applied Mathematics and Computer Science, 11, 223–236.

Zhang, Z., Al-Abri, S., and Zhang, F. (2021). Dissensus algorithms for opinion dynamics on the sphere. In 2021 60th IEEE Conference on Decision and Control (CDC), 5988–5993.

## Appendix A. DERIVATION OF NEGATIVE EIGENVALUES FOR CONSENSUS JACOBIAN MATRIX

Here we provide the details on how to derive $$\lambda_2 = -\frac{2N-1}{N}$$ and $$\lambda_3 = -1$$ for the Jacobian matrix at the consensus equilibrium.

We can construct $$N$$ linearly independent vectors $$\boldsymbol{y}^m = [\boldsymbol{y}_1^{m\top}, \boldsymbol{y}_2^{m\top}, \cdots, \boldsymbol{y}_N^{m\top}]^\top \in \mathbb{R}^{Nd}$$ with $$\boldsymbol{y}_n^m \in \mathbb{R}^d$$ where $$\boldsymbol{y}_m^m = \boldsymbol{v}_c^*$$ and $$\boldsymbol{y}_n^m = \boldsymbol{0}$$ if $$n \neq m$$ for $$m, n = 1, \cdots, N$$. Then

$$\frac{\partial f}{\partial \boldsymbol{v}} \boldsymbol{y}^m = \left[\left(\sum_{k \neq i} \frac{\partial f_i}{\partial \boldsymbol{v}_k} \boldsymbol{y}_k^m\right) + \frac{\partial f_i}{\partial \boldsymbol{v}_i} \boldsymbol{y}_i^m\right]_{\forall i}$$

$$= \left[\left(\sum_{k \neq i} \frac{1}{N} (\boldsymbol{I} - \boldsymbol{v}_c^* \boldsymbol{v}_c^{*\top}) \boldsymbol{y}_k^m\right) - \frac{N-1}{N} (\boldsymbol{v}_c^* \boldsymbol{v}_c^{*\top} + \boldsymbol{I}) \boldsymbol{y}_i^m\right]_{\forall i}$$

$$= \left[-\frac{2N-2}{N} \boldsymbol{y}_i^m\right]_{\forall i} = -\frac{2N-2}{N} \boldsymbol{y}^m,\tag{A.1}$$

where we use $$(\boldsymbol{I} - \boldsymbol{v}_c^* \boldsymbol{v}_c^{*\top})\boldsymbol{0} = \boldsymbol{0}$$, $$(\boldsymbol{I} - \boldsymbol{v}_c^* \boldsymbol{v}_c^{*\top})\boldsymbol{v}_c^* = \boldsymbol{0}$$, $$(\boldsymbol{v}_c^* \boldsymbol{v}_c^{*\top} + \boldsymbol{I})\boldsymbol{0} = \boldsymbol{0}$$ and $$(\boldsymbol{v}_c^* \boldsymbol{v}_c^{*\top} + \boldsymbol{I})\boldsymbol{v}_c^* = 2\boldsymbol{v}_c^*$$. This means that $$\lambda_2 = -\frac{2N-2}{N}$$ has algebraic multiplicity of $$N$$.

Denote $$\boldsymbol{z}_1, \boldsymbol{z}_2, \cdots, \boldsymbol{z}_N$$ to be vectors in $$\mathbb{R}^d$$. For any vector $$\boldsymbol{z} = [\boldsymbol{z}_1^\top, \boldsymbol{z}_2^\top, \cdots, \boldsymbol{z}_N^\top]^\top \in \mathbb{R}^{Nd}$$ perpendicular to $$\boldsymbol{x}^l$$ and $$\boldsymbol{y}^m$$ for all $$l = 1, \cdots, d-1$$ and $$m = 1, \cdots, N$$, $$\boldsymbol{z}$$ satisfies $$N + d - 1$$ linearly independent equality constraints. This means that $$\sum_{i=1}^N \boldsymbol{z}_i^\top \boldsymbol{w}^l = 0$$, $$\forall l = 1, \cdots, d-1$$ and $$b_m \boldsymbol{z}_i^\top \boldsymbol{v}_c^* = 0$$, $$\forall i = 1, \cdots, N$$, which implies that $$\sum_{i=1}^N \boldsymbol{z}_i = \boldsymbol{0}$$. Then

$$\frac{\partial f}{\partial \boldsymbol{v}} \boldsymbol{z} = \left[\left(\sum_{k \neq i} \frac{\partial f_i}{\partial \boldsymbol{v}_k} \boldsymbol{z}_k\right) + \frac{\partial f_i}{\partial \boldsymbol{v}_i} \boldsymbol{z}_i\right]_{\forall i}$$

$$= \left[\left(\sum_{k \neq i} \frac{1}{N} (\boldsymbol{I} - \boldsymbol{v}_c^* \boldsymbol{v}_c^{*\top}) \boldsymbol{z}_k\right) - \frac{N-1}{N} (\boldsymbol{v}_c^* \boldsymbol{v}_c^{*\top} + \boldsymbol{I}) \boldsymbol{z}_i\right]_{\forall i}$$

$$= \left[\left(\sum_{k \neq i} \frac{1}{N} \boldsymbol{z}_k\right) - \frac{N-1}{N} \boldsymbol{z}_i\right]_{\forall i} = [-\boldsymbol{z}_i]_{\forall i} = -\boldsymbol{z}.\tag{A.2}$$

Since $$\boldsymbol{z}$$ satisfies $$N + d - 1$$ linearly independent equality constraints, the eigenvalue $$\lambda_3 = -1$$ has algebraic multiplicity $$Nd - N - d + 1$$.

Therefore, the Jacobian matrix $$\frac{\partial f}{\partial \boldsymbol{v}}$$ has eigenvalues $$\lambda_2 = -\frac{2N-2}{N}$$ with algebraic multiplicity of $$N$$ and $$\lambda_3 = -1$$ with algebraic multiplicity $$Nd - N - d + 1$$.

## Appendix B. DERIVATION OF POSITIVE EIGENVALUES FOR JACOBIAN MATRIX OF ORTHOGONAL DISSENSUS

Construct a vector $$\boldsymbol{x} = [\boldsymbol{x}_1^\top, \boldsymbol{x}_2^\top, \cdots, \boldsymbol{x}_N^\top]^\top \in \mathbb{R}^d$$ with $$\boldsymbol{x}_i \in \mathbb{R}^d$$ for all $$i$$ as follows,

$$\boldsymbol{x}_i \triangleq \begin{cases} b_i \lambda_{s_2} \boldsymbol{v}_{s_2}^*, & \text{if } i \in \mathcal{V}_{s_1}, \\ c_i \lambda_{s_1} \boldsymbol{v}_{s_1}^*, & \text{if } i \in \mathcal{V}_{s_2}, \\ \boldsymbol{0}, & \text{otherwise}, \end{cases}\tag{B.1}$$

where $$b_i \triangleq \boldsymbol{v}_i^\top \boldsymbol{v}_{s_1}^* \in \{\pm 1\}$$, $$c_i \triangleq \boldsymbol{v}_i^\top \boldsymbol{v}_{s_2}^* \in \{\pm 1\}$$. For $$i \notin (\mathcal{V}_{s_1} \cup \mathcal{V}_{s_2})$$, $$\boldsymbol{x}_i = \boldsymbol{0}$$ and $$\boldsymbol{v}_i \perp \boldsymbol{v}_{s_1}^*$$, $$\boldsymbol{v}_i \perp \boldsymbol{v}_{s_2}^*$$

$$\sum_{k \neq i} \frac{\partial f_i}{\partial \boldsymbol{v}_k} \boldsymbol{x}_k + \frac{\partial f_i}{\partial \boldsymbol{v}_i} \boldsymbol{x}_i = \sum_{k \in \mathcal{V}_{s_1} \cup \mathcal{V}_{s_2}} \frac{1}{N} \boldsymbol{v}_k \boldsymbol{v}_i^\top \boldsymbol{x}_k = \boldsymbol{0}.\tag{B.2}$$

For $$i \in (\mathcal{V}_{s_1} \cup \mathcal{V}_{s_2})$$, suppose $$i \in \mathcal{V}_{s_1}$$. Then $$\boldsymbol{x}_i = b_i \lambda_{s_2} \boldsymbol{v}_{s_2}^*$$

$$\sum_{k \neq i} \frac{\partial f_i}{\partial \boldsymbol{v}_k} \boldsymbol{x}_k + \frac{\partial f_i}{\partial \boldsymbol{v}_i} \boldsymbol{x}_i$$

$$= (\lambda_{s_1} - \frac{1}{N}) \boldsymbol{x}_i + \lambda_{s_1} \boldsymbol{x}_i + (\frac{1}{N} - \lambda_{s_1} + \lambda_{s_2}) \boldsymbol{x}_i$$

$$= (\lambda_{s_1} + \lambda_{s_2}) \boldsymbol{x}_i.\tag{B.3}$$

If $$i \in \mathcal{V}_{s_2}$$, $$\sum_{k \neq i} \frac{\partial f_i}{\partial \boldsymbol{v}_k} \boldsymbol{x}_k + \frac{\partial f_i}{\partial \boldsymbol{v}_i} \boldsymbol{x}_i = (\lambda_{s_1} + \lambda_{s_2}) \boldsymbol{x}_i$$.

Then $$\frac{\partial f}{\partial \boldsymbol{v}} \boldsymbol{x} = \left[\left(\sum_{k \neq i} \frac{\partial f_i}{\partial \boldsymbol{v}_k} \boldsymbol{x}_k\right) + \frac{\partial f_i}{\partial \boldsymbol{v}_i} \boldsymbol{x}_i\right]_{\forall i} = (\lambda_{s_1} + \lambda_{s_2}) \boldsymbol{x}$$.

Therefore, the Jacobian matrix $$\frac{\partial f}{\partial \boldsymbol{v}}$$ has one positive eigenvalue $$(\lambda_{s_1} + \lambda_{s_2})$$.