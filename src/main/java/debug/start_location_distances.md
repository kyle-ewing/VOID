# Start Location Distances

Distance from each starting base to every other starting base on the BASIL pool. All values in **pixels**. **A\*** = summed path length along the A* route;
**euclidean** = straight-line center-to-center.

## Distance ranges per map

Min / avg / max of A* and euclidean across all start-to-start distances on each map, sorted by
A* avg ascending.

| Map | Starts | A* min | A* avg | A* max | euc min | euc avg | euc max |
|---|---|---|---|---|---|---|---|
| Python 1.1 | 4p | 4137 | 4507 | 5332 | 1423 | 2846 | 3841 |
| Neo Moon Glaive 2.1 | 3p | 4852 | 4901 | 4974 | 3220 | 3337 | 3520 |
| Empire of the Sun 1.0 | 4p | 4307 | 4956 | 5673 | 3520 | 3943 | 4695 |
| Roadrunner_SE 1.2 | 4p | 4710 | 5006 | 5557 | 2688 | 3261 | 4212 |
| La Mancha 1.1 | 4p | 4543 | 5092 | 5906 | 3456 | 3880 | 4623 |
| Fighting Spirit 1.3 | 4p | 4618 | 5114 | 5853 | 3520 | 3884 | 4636 |
| Circuit Breakers 1.0 | 4p | 4458 | 5185 | 5801 | 3488 | 3867 | 4593 |
| Heartbreak Ridge 1.1 | 2p | 5194 | 5194 | 5194 | 3520 | 3520 | 3520 |
| Andromeda 1.0 | 4p | 4749 | 5230 | 5907 | 3520 | 3923 | 4695 |
| Jade 1.0 | 4p | 4714 | 5237 | 5796 | 3488 | 3874 | 4606 |
| Destination 1.1 | 2p | 5295 | 5295 | 5295 | 3683 | 3683 | 3683 |
| Tau Cross 1.1 | 3p | 5383 | 5606 | 5766 | 3452 | 3539 | 3678 |
| Benzene 1.1 | 2p | 6318 | 6318 | 6318 | 4272 | 4272 | 4272 |

**Outliers**
- **Python**: euclidean min 1423px (1 o'clock to 2 o'clock) is far below every other pair, yet
  its A* (4137) is normal. Two bases are physically close but walled off, so euclidean badly
  under-ranks the real distance. Its euclidean avg (2846) and A* avg (4507) are the lowest of any
  4p map.
- **Roadrunner**: euclidean min 2688px is also low relative to a normal A* min of 4710 (short
  straight line, normal path); same straight-line-vs-path mismatch as Python, milder.
- **Benzene**: A* 6318px is the single longest distance in the whole pool (longest rush
  distance), well above the next 2p map (Destination 5295).
- **Empire of the Sun**: lowest A* min (4307) among the standard 4-corner maps, from its close
  horizontal-edge pairs.

## Summary

| Map | Starts | short-edge A* spread | diagonal A* spread | A* start-sensitivity |
|---|---|---|---|---|
| Circuit Breakers 1.0 | 4p | **917** | 26 | high; *tall* map: vertical neighbor ~4480, horizontal ~5280 |
| Empire of the Sun 1.0 | 4p | **662** | 18 | high; *wide* map: horizontal neighbor ~4310, vertical ~4900 |
| Jade 1.0 | 4p | 463 | 74 | moderate |
| Andromeda 1.0 | 4p | 408 | 64 | moderate |
| Fighting Spirit 1.3 | 4p | 320 | 180 | moderate; right edge 4618 vs left 4938 |
| La Mancha 1.1 | 4p | 279 | 122 | moderate |
| Roadrunner_SE 1.2 | 4p | 275 | **561** | moderate edges, strong diagonal/rotational skew |
| Python 1.1 | 4p | 147 | 456 | moderate diagonal |
| Tau Cross 1.1 | 3p | 383 | n/a | moderate |
| Neo Moon Glaive 2.1 | 3p | 122 | n/a | negligible |
| Benzene 1.1 | 2p | n/a | n/a | n/a |
| Destination 1.1 | 2p | n/a | n/a | n/a |
| Heartbreak Ridge 1.1 | 2p | n/a | n/a | n/a |

## Notes
- Fully covered (all spawns logged directly): Andromeda, Circuit Breakers, Fighting Spirit,
  Jade, La Mancha, Roadrunner, both 3p maps, all three 2p maps.
- One spawn never logged directly (row reconstructed via symmetry): **Empire of the Sun** (BL),
  **Python** (8 o'clock).
