Real-Time Timing Analysis
=========================

A collection of the *Composite@GWU* group's real-time timing analysis software that is used in our papers (both published, and in submission).

This includes 

- `/fault_tolerance/rta/` -- a response time analysis for system-level fault tolerance.
- `/fault_tolerance/rtc/` -- settle-time analysis using real-time calculus based on the work at EPFL.
- `/parallel/splitmerge/` -- Timing analysis using a holistic analysis.  Also includes code for "reduction based schedulability analysis" by Abdelzaher et al, and some assignment algorithms.  *Qi, can we split this up at all without taking up too many cycles?*

Usage
-----

See the `Readme.md` in each subdirectory for more information.  Note that none of this code is maintainable in a general sense.  We will continue to use it, but it will be very difficult for a 3rd party to jump in and use it.  Please contact us if you're interested on the *Composite* mailing list.

License
-------

All of this code is ruled under the same license as *Composite*: GPL v2.  If you are a company, and this prevents you from using the code, please talk to us.

```
Real-Time Timing Analysis Simulations
Copyright (C) 2013 Qi Wang, Jiguo Song, Gabriel Parmer

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
```
