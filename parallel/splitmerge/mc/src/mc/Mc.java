/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mc;

import java.util.*;
import java.util.Random;

/**
 *
 * @author Qi Wang For multicore Composite schedulability analysis!
 */
final class Config {
    //async params

    double async_r = 1;
    double async_return = 0.5;
    int in_order_evt = 1;
    int method; //0 -> RTA only; 1 -> Praveen Original; 2-> optimized Praveen
    // 3-> holistic
    int ncpus = 32;
    /*graph params*/
    int depth = 3;
    int width = 20; // this is max width
    int width_mean = 10;
    int width_var = width_mean / 10;
    double invs_mean = 2;// # of children per comp
    double invs_var = invs_mean / 10;
    int invs_max = 10;
    double inv_jump_mean = 1;
    double inv_jump_var = 0;//inv_jump_mean / 2;
    long e_mean = 0;
    long e_var = e_mean; // length variance 
    long e_max = Long.MAX_VALUE;
    //release time: 0 - e, even dist
    int split = 1;
    int avg_over = 100;
    /*task params*/
    int ntasks = 0;
    double totU = 0;
    //task individule u: normal dist
    //double u_mean = (double)totU/ntasks;
    double u_var = 0;
    //task individule u: even between 0 - max    
    double u_mean = 0;
    double u_min = 0;
    double u_max = 0;//= (double) totU / ntasks * 2;//1;//Integer.MAX_VALUE;
    double home_level_mean = 1;
    double home_level_var = 0;
    int call_freq = 2;
    double d_ratio = -1;//20; // d = wcet * config.d_ratio;
    // No cost case:
//    long cost_ipi_top_half = 0; // taken care of. using high prio threads
//    long cost_ipi_bottom_half = 0; // taken care of! in execution
//    long cost_ipi_sending = 0; // taken care of! in execution
//    long cost_inv = 0; // taken care of! in execution
//    long cost_async_evt_trigger = 0;
//    long cost_async_evt_wait = 0;
//    long cost_async_parent_up = 0;
    //
    //
    long cost_cache_migration = 2408;
    /* Composite costs: */
//    long cost_ipi_top_half = 1692; // taken care of. using high prio threads
//    long cost_ipi_bottom_half = 736 + cost_cache_migration; // taken care of! in execution
//    long cost_ipi_sending = 968; // taken care of! in execution
    //    long cost_inv = 41; // taken care of! in execution
    /* Composite costs w/o MPD: */
//    long cost_inv = 900; // taken care of! in execution
    //
    /* Linux costs: */
//    long cost_ipi_top_half = 0; //Linux doesn't have it!
//    long cost_ipi_bottom_half = 4484 + cost_cache_migration; // taken care of! in execution
//    long cost_ipi_sending = 5928; // taken care of! in execution
//    long cost_inv = 17736; // taken care of! in execution
    //
    //
    //
    /* ASYNC split/merge case: Composite costs: */
    long cost_ipi_top_half = 1692; // taken care of. using high prio threads
    long cost_ipi_bottom_half = 736 + cost_cache_migration; // taken care of! in execution
    long cost_ipi_sending = 968; // taken care of! in execution
    long cost_async_evt_trigger = 2608;
    long cost_async_evt_wait = 2264;
    long cost_async_parent_up = 4672;
    long cost_inv = 900; // taken care of! in execution
//    
    //
    //
    //
    int do_partition = 0;
    int do_global = 0;
    int do_comp2core = 0;
    //
    int do_async = 0;
    int do_fj = 1;
    int async_best_fit = 0;
    int maxpower = 10;
    int task_order = 2; // 0-> high u, 1-> low prio, 2-> high prio
    int fork_join = 0;
    int fork_join_max_seg = 0;
    int FJ_nomain = 0;
    int implicit_deadline = 1;
    int async_only_partiton = 0;
    double async_release_offset = 1;
    int async_naive_assign = 0;

    public void set_U(double u) {
        totU = u;

        //u_min = this.u_min;
        //u_max = this.u_max;//u_mean * 2;//(double) totU / ntasks * 2;
        //u_mean = u_min + (u_max - u_min) / 2;
        ntasks = (int) Math.ceil(u / u_mean);
        //System.out.println("..."+ntasks);
    }
}

class Graph {

    int w;
    int d;
    component[][] g;
    int[] validw; // actual width
    Task[] task;
    Core[] core;
    Config config;
    Assign global_a;// for recursive convenience only.
    int thd_core[][];

    class Assigned_comp {

        component c;
        Assigned_comp next;
    }

    class Core {

        double u;
        int id;
        Assign assigned_head[];
        double sum_assign[];
        Assigned_comp ac;

        public Core(int i) {
            this.id = i; // -1 -> virtual
        }
    }

    class Async_assign {

        Assign a;
        Async_assign next;

        public Async_assign() {
        }
    }

    class Assign {

        component c;
        Core cpu;
        Task thd;
        Exe exe;
        Assign next;
        Assign orig;
        int async_idx;
        int parent_async_idx;
        //boolean call_dep;

        public Assign() {
        }

        void copy(Assign a) {
            c = a.c;
            cpu = a.cpu;
            thd = a.thd;
            exe = a.exe;
            next = a.next;
        }

        public Assign(component c1) {
            c = c1;
            //int i;
//            this.exe = new Exe();
//            Exe e = this.exe;
//            for (i = 0; i < c.ninv; i++){
//                e.next = new Exe();
//            }
        }
    }

    class inv {

        int i, j;
        component c;
        long release, relative_r;
        //int produce = 1;
        int async; // whether this invocation can be asynchrony
        int async_return;
        int fj_nseg = 0;
        long handling_exe;
        sync_point sync_point;
        //int parallelism = 1; // we have this always 1, different from forkjoin
        //int cost = config.ipi_c;

        public inv(int xx, int yy, int r, int async, int async_return, long handling_exe) {
            this.i = xx;
            this.j = yy;
            this.c = g[i][j];
            this.release = r;
            this.async = async;
            this.async_return = async_return;
            this.handling_exe = handling_exe;
        }
    }

    class Exe {

        long e_t;
        long inv_cost;
        inv call;
        Core cpu;
        Assign assign;
        Exe next;
        component c;
        inv sync_call = null;
        int fj_seg = 0;

        public Exe(long e, Core cpu, component c, inv i, Assign a) {
            //assert (e > 0);
            assign = a;
            e_t = e;
            assert (cpu != null);
            if (cpu != null) {
                this.cpu = cpu;
            }
//            System.out.println(e);
            this.c = c;
            call = i;
        }

        public Exe() {
        }
    }

    class sync_point {

        double t;
        inv inv;
    }

    class component {

        int x, y;
        long e;
        int parallelism;
        int ninv;
        inv[] invlist;
        int[][] dest;
        int parents = 0;
        int dead = 0;
        double thd_u[];
        double comp_u;
        Sub_thd thds;
        Sub_thd end;
        boolean assigned;
        sync_point[] sync_points;
        int n_async;

        public component(int xx, int yy) {
            x = xx;
            y = yy;
        }
    }

    class Fold {

        int length = 0;
        Task t;
        Core stage[] = new Core[config.ncpus];
        int cpu[] = new int[config.ncpus];
        long e[] = new long[config.ncpus];
        Fold next = null;

        public void add(Task t, Core cpu) {
            this.length++;
            if (this.t != null) {
                assert (this.t == t);
                assert (t.folds >= 1);
            } else {
                this.t = t;
            }
            this.stage[length - 1] = new Core(cpu.id);
            assert (this.cpu[cpu.id] == 0);
            this.cpu[cpu.id] = 1;
        }
    }

    class Seg extends Fold {

        int prio = 0;
        Task seg_t;
        Seg next;
        int type; // 0 -> forward, 1 -> cross, 2-> reverse
        long cmax = 0;
        long v_e;

        @Override
        public void add(Task t, Core cpu) {
            assert (false);
        }

        public void add(Task t, Core cpu, long e) {
            this.length++;
            if (this.t != null) {
                assert (this.t == t);
                assert (t.folds >= 1);
            } else {
                this.t = t;
                cmax = 0;
            }
            this.stage[length - 1] = new Core(cpu.id);
            assert (this.cpu[cpu.id] == 0);
            this.cpu[cpu.id] = 1;
            this.e[length - 1] = e;

            if (e > cmax) {
                cmax = e;
            }

        }
    }

    class Core_node {

        Core c;
        long e;
        double jitter, max_r;
        Core_node next;
    }

    class Task {

        double p, d, u;
        int id;
        long wcet = 0;
        component home;
        Assign assign_head;
        long assign_length;
        Exe exe_head;
        Fold fold_list;
        int folds = 0;
        Seg high_segs;
        int segs;
        Path path;
        long[] nodemax;
        long v_e;
        int assigned;
        //int tmp;
        int alloc_core[];
        long comp_access_a[][];
        long comp_access_e[][];
        // used w/ async sub tasks
        int n_async;
        Task async_subt[];
        long rt;
        Async_assign async_chain;
        Async_assign async_tail;
        Task async_parent[];
        long async_release, async_merge;
        inv async_call;
        int sibling_if_taken;
        int async_ncpus;
        long last_rt;

        int add_async(Assign a) {
            async_tail.next = new Async_assign();
            async_tail = async_tail.next;
            async_tail.a = a;
            return 0;
        }

        public class Path extends Seg {

            Core_node stage_list = new Core_node();

            @Override
            public void add(Task t, Core cpu, long e) {
                Core_node cn;
                assert (t.path == this);
                t.path.length++;
                int l = t.path.length;

                if (this.t != null) {
                    assert (this.t == t);
                    assert (t.folds >= 1);
                } else {
                    this.t = t;
                    this.stage_list = new Core_node();
                }
                cn = stage_list;
                for (int i = 0; i < l - 1; i++) {
                    cn = cn.next;
                }

                assert (cn.next == null);
                cn.next = new Core_node();
                cn = cn.next;
                cn.c = new Core(cpu.id);
                cn.e = e;

                //    assert (this.cpu[cpu.id] == 0);
                this.cpu[cpu.id]++;
                //System.out.print(cpu.id + ",");
            }
        }

        void calc_path() {
            int i, j, k;
            Fold f = this.fold_list.next;
            assert (f != null);
            this.path = new Path();
            this.alloc_core = new int[config.ncpus];
            double sum = 0;
            //System.out.println("\nthd" + this.id);
            for (j = 0; j < this.folds; j++) {//calc path
                for (k = 0; k < f.length; k++) {
                    assert (f.stage[k] != null);
                    this.alloc_core[f.stage[k].id] = 1;
                    path.add(this, f.stage[k], f.e[k]);
                    sum += f.e[k];
                }
                f = f.next;
            }// path done
            Core_node cn = this.path.stage_list.next;
//            if (this.path.length > 1) {
//                int ij = 0;
//                while (cn != null) {
//                    System.out.print(cn.c.id + ",");
//                    cn = cn.next;
//                    ij++;
//                }
//                assert(ij == this.path.length);
//                System.out.println();
//            }

            assert (sum >= this.wcet);
            path.stage = new Core[path.length];
            Core_node cl = path.stage_list.next;
            assert (cl != null);

            for (j = 0; j < this.path.length; j++) {
                path.stage[j] = cl.c;
                cl = cl.next;
            }
            assert (cl == null);
        }

        void calc_high_segs() {
            int i, j, k;

            Fold f = this.fold_list.next;
            this.calc_path();

            if (this.u > 1) {
                if (config.implicit_deadline == 0) {
                    assert (d > p);
                }
                i = id;
            } else {
                assert (d == p);
                i = id - 1;
            }
            this.segs = 0;
            this.high_segs = new Seg();

            for (; i >= 0; i--) {
                if (task[i].assigned == 0) {
                    continue;
                }

                f = task[i].fold_list.next;
                assert (f != null);
                for (j = 0; j < task[i].folds; j++, f = f.next) {
                    int m, n;
                    // for this fold...
                    int[] taken = new int[f.length];
                    for (m = 0; m < f.length; m++) {
                        int max_l = 0, max_n = 0;
                        for (n = 0; n < this.path.length; n++) {

                            if (f.stage[m].id == path.stage[n].id) {
                                int l = 0;
                                while (l + m < f.length && n + l < path.length) {
                                    assert (f.stage[m + l] != null);
                                    assert (path.stage[n + l] != null);

                                    if (f.stage[m + l].id
                                            != path.stage[n + l].id) {
                                        break;
                                    }
                                    l++;
                                }
                                if (l > max_l) {
                                    max_l = l;
                                    max_n = n;
                                }
                            }
                        }

                        if (max_l > 0) {
                            // new seg!
                            assert (m + max_l - 1 < f.length);
                            Seg s = this.high_segs;
                            int ii;
                            for (ii = 0; ii < this.segs; ii++) {
                                s = s.next;
                            }
                            s.next = new Seg();
                            s = s.next;
                            segs++;
                            //System.out.println("\nnew seg");
                            for (ii = 0; ii < max_l; ii++) {
                                taken[m + ii] = 1;
                                s.add(task[i], f.stage[m + ii], f.e[m + ii]);
                                //System.out.print(f.stage[m + ii].id);
                                if (max_l > 1) {
                                    s.type = 0; //forward
                                    //System.out.println("forward flow segment found!!");
                                } else {
                                    s.type = 1; //cross
                                }
                            }
                            m += (max_l - 1);
                        }
                        // forward and cross flow done
                    }

                    // reverse flow next...
                    for (m = 0; m < f.length; m++) {
                        if (taken[m] == 1) {
                            continue;
                        }

                        int max_l = 0, max_n = 0;
                        for (n = this.path.length - 1; n >= 0; n--) {

                            if (f.stage[m].id == path.stage[n].id) {
                                int l = 0;
                                while (l + m < f.length && n - l >= 0) {
                                    assert (f.stage[m + l] != null);
                                    assert (path.stage[n - l] != null);

                                    if (f.stage[m + l].id
                                            != path.stage[n - l].id) {
                                        break;
                                    }
                                    l++;
                                }
                                if (l > max_l) {
                                    max_l = l;
                                    max_n = n;
                                }
                            }
                        }

                        if (max_l > 1) { // Cross flow already taken care of
                            // new seg!
                            assert (m + max_l - 1 < f.length);
                            Seg s = this.high_segs.next;
                            int ii, jj;
                            boolean found = false;
                            for (ii = 0; ii < this.segs; ii++) {
                                if (s.length == max_l && found == false) {
                                    boolean equal = true;
                                    for (jj = 0; jj < max_l; jj++) {
                                        if (s.stage[jj].id != f.stage[m + jj].id) {
                                            equal = false;
                                            break;
                                        }
                                    }
                                    if (equal == true) {
                                        found = true;
                                    }
                                }
                                s = s.next;
                            }

                            assert (found == true); // I believe this doesn't exsit. Let's check...
                            assert (s.next == null);
                            if (found == false) {
                                s.next = new Seg();
                                s = s.next;
                                segs++;
                                for (ii = 0; ii < max_l; ii++) {
                                    s.add(task[i], f.stage[m + ii], f.e[m + ii]);
                                    //System.out.print(f.stage[m + ii].id);
                                    s.type = 2;

                                    //System.out.println("reverse flow segment found!!");
                                }
                                assert (s.length > 1);
                                m += (max_l - 1);
                            }
                        }
                    }
                    // reverse flow done

                }
            }
        }

        void fold_init() {
            fold_list = new Fold();
            folds = 0;
            high_segs = new Seg();
            segs = 0;
        }

        final long calc_e(component c, Task t) {
            int i;

            long e_tot = c.e;
            t.comp_access_a[c.x][c.y]++;
            long n_access = t.comp_access_a[c.x][c.y];
            if (config.fork_join > 0) {
                n_access = 1;
            }
            if (c.ninv > 0 && (n_access % config.call_freq == 1)) {
                for (i = 0; i < c.ninv; i++) {
                    int j = 0;
                    do {
                        j++;
                        e_tot += calc_e(g[c.invlist[i].i][c.invlist[i].j], t);
                    } while (j < c.invlist[i].fj_nseg);
                }
            }
            //System.out.print("+");

            assert (e_tot > 0);
            return e_tot;
        }

        public Task(component c, double util) {
            home = c;
            u = util;
            assert (u <= config.u_max);
            this.comp_access_a = new long[config.depth][config.width];
            wcet = this.calc_e(home, this);

            p = ((double) wcet) / u;
            if (u <= 1 || config.implicit_deadline > 0) {
                d = p;
            } else {
                d = wcet * config.d_ratio;
            }

            //System.out.println("thd " + p + " " + e);
        }

        public Task() {
            //used for subtask 
        }
    }

    // from 1 to max, normal dist like.
    long get_rnd(double mean, double max, double variance) {
        double tmp;
        long ret;
        Random r = new Random();
        do {
            tmp = r.nextGaussian() * variance;
//            if (tmp > Math.floor(tmp) + 0.5) {
//                tmp = (int) Math.floor(tmp) + 1;
//            } else {
//                tmp = (int) Math.floor(tmp);
//            }
            ret = (int) Math.round(mean + tmp);
        } while (ret <= 0 || ret > max);
        return ret;
    }

    double get_rnd_double(double mean, double max, double variance) {
        double tmp, ret;
        Random r = new Random();
        do {
            tmp = r.nextGaussian() * variance;
            ret = mean + tmp;
        } while (ret <= 0 || ret > max);
        return ret;
    }

    // 0 to max-1  
    long get_rnd_even(long max) {
        Random r = new Random();
        return r.nextInt((int) max);
    }

    double get_rnd_even_double(double max, double min) {
        Random r = new Random();
        double ret;
        do {
            ret = min + r.nextDouble() * (max - min);
        } while (ret <= 0 || ret >= max);
        return ret;
    }

    void generate_graph() {
        g = new component[d][w];
        validw = new int[d];

        int i, j;
        long rnd;
        for (i = 0; i < d; i++) {
            rnd = get_rnd(config.width_mean, w, config.width_var);
            validw[i] = (int) rnd;
            for (j = 0; j < rnd; j++) {
                component c = new component(i, j);
                g[i][j] = c;
                c.dest = new int[d][w];
                do {
                    c.e = this.get_rnd(config.e_mean, config.e_max, config.e_var);
                } while (c.e < 50);
                if (config.FJ_nomain == 1 && i == 0) {
                    c.e = 0;
                }
            }
        }
//        for (i = 0; i < w; i++) {
//            System.out.println(i + ":" + validw[i]);
//        }
    }

    int generate_invs() {
        int i, j, k, m, jump;

        int[] comp_left = new int[d];

        comp_left[d - 1] = 0;
        for (i = d - 2; i >= 0; i--) {
            comp_left[i] += validw[i] + comp_left[i + 1];
        }

        for (i = 0; i < d - 1; i++) {
            for (j = 0; j < validw[i]; j++) {
                assert (g[i][j] != null);
                component c = g[i][j];
                do {
                    m = (int) this.get_rnd(config.invs_mean, config.invs_max, config.invs_var);
                } while (m > comp_left[i]);
                g[i][j].ninv = m;
//                if (config.FJ_nomain == 1) {
//                    System.out.println(m);
//                }
                assert (m > 0);

                c.invlist = new inv[m];
                for (k = 0; k < m; k++) {
                    int ii, jj;
                    do {
                        jump = (int) this.get_rnd(config.inv_jump_mean, d, config.inv_jump_var);
                    } while (jump + i > d - 1);
                    ii = jump + i;
                    jj = (int) this.get_rnd_even(validw[ii]);
                    c.dest[ii][jj] = 1;
                    long tmp;
                    int redo;
                    int iter = 0;
                    do {
                        iter++;
                        if (iter > 10) {
                            return -1;
                        }
                        if (c.e == 0) {
                            tmp = 0;
                            break;
                        }
                        // release_offset, release time....                        
                        if (config.async_release_offset < 1) {
                            tmp = (long) this.get_rnd_even_double(c.e * (config.async_release_offset + 0.05), c.e * (config.async_release_offset - 0.05));
                        } else {
                            tmp = this.get_rnd_even(c.e);
                        }
                        redo = 0;
                        for (int kk = 0; kk < k; kk++) {
                            if (tmp == c.invlist[kk].release || c.e - tmp < 10) {
                                redo = 1;
                            }
                        }
                    } while (redo == 1 || tmp == 0);
                    assert (tmp >= 0);
                    int async, async_return;
                    //if (this.get_rnd_even_double(1, 0) > config.async_r || c.x >= 1) {
                    if (this.get_rnd_even_double(1, 0) > config.async_r) {
                        async = 0;
                    } else {
                        async = 1;
                    }
                    //System.out.println(async);
                    if (this.get_rnd_even_double(1, 0) > config.async_return) {
                        async_return = 0;
                    } else {
                        async_return = 1;
                    }
                    if (async == 1 && config.fork_join > 0) {
                        async_return = 1;
                    }
                    long handling = 0;
//                    do {
//                        handling = get_rnd_even(c.e - tmp);
//                    } while (handling == 0); // not gonna use this. for now...
                    //assert (handling > 0 && handling + tmp < c.e);
                    c.invlist[k] = new inv(ii, jj, (int) tmp, async, async_return, handling);
                    assert (c.invlist[k].release <= c.e && c.invlist[k].release >= 0);
                    if (async == 1 && config.fork_join > 0) {
                        assert (async_return == 1);
                        do {
                            c.invlist[k].fj_nseg = (int) get_rnd_even(config.fork_join_max_seg);
                        } while (c.invlist[k].fj_nseg < 2);
                        //System.out.println(c.invlist[k].fj_nseg);
                    } else {
                        assert (c.invlist[k].fj_nseg == 0);
                    }
                    g[ii][jj].parents++;
                }
                int kk;
                // sort according to release time
                for (k = 0; k < m - 1; k++) {
                    for (kk = k + 1; kk < m; kk++) {
                        if (c.invlist[k].release > c.invlist[kk].release) {
                            inv temp;
                            temp = c.invlist[k];
                            c.invlist[k] = c.invlist[kk];
                            c.invlist[kk] = temp;
                        }
                    }
                    //System.out.println(c.invlist[k].release + "in "+ c.e);
                }
                if (config.FJ_nomain > 0) {
                    for (k = 0; k < m; k++) {
                        c.invlist[k].release = c.e;
                        if (k == 0) {
                            c.invlist[k].relative_r = c.e;
                        } else {
                            c.invlist[k].relative_r = 0;
                        }
                    }
                }
                k = 0;
                c.invlist[k].relative_r = c.invlist[k].release;
                assert (c.invlist[k].relative_r >= 0);

                for (k = 1; k < m; k++) {
                    c.invlist[k].relative_r = c.invlist[k].release - c.invlist[k - 1].release;
                    assert (c.invlist[k].relative_r >= 0);
                }
                //System.out.println(":" + ",");
                c.n_async = 0;
                for (k = 0; k < m; k++) {
                    if (c.invlist[k].async == 1) {
                        c.n_async++;
                    }
                }
                c.sync_points = new sync_point[m];
                int n_async = 0;
                int iter = 0;
                double temp;
                for (k = 0; k < m; k++) {
                    inv thisinv = c.invlist[k];
                    if (thisinv.async == 1) {
                        c.sync_points[n_async] = new sync_point();
                        c.sync_points[n_async].inv = thisinv;
                        thisinv.sync_point = c.sync_points[n_async];
                        if (thisinv.async_return == 1 && config.do_fj == 0) {
                            c.sync_points[n_async].t = get_rnd_even_double(1, 0);
//                                do {
//                                    iter++;
//                                    if (iter > 10) {
//                                        return -1;
//                                    }
//                                    if (config.in_order_evt == 1 && n_async > 0) {
//                                        if (c.sync_points[n_async - 1].t > thisinv.release) {
//                                            temp = c.sync_points[n_async - 1].t + get_rnd_even(c.e - c.sync_points[n_async - 1].t);
//                                        } else {
//                                            temp = thisinv.release + get_rnd_even(c.e - c.sync_points[n_async - 1].t);
//                                        }
//                                    } else {
//                                        temp = thisinv.release + get_rnd_even(c.e - thisinv.release);
//                                    }
//                                    int zz;
//                                    for (zz = 0; zz < n_async; zz++) {
//                                        if (c.sync_points[zz].t == temp) {
//                                            temp = 0;
//                                        }
//                                    }
//                                    if ((c.e - temp) < (c.n_async - n_async)) {
//                                        temp = 0;
//                                    }
//                                    if (config.in_order_evt == 1 && n_async > 0) {
//                                        if (!(temp > c.sync_points[n_async - 1].t)) {
//                                            continue;
//                                        }
//                                    }
//                                    if (temp <= thisinv.release && temp >= c.e) {
//                                        continue;
//                                    }
//                                    int redo = 0;
//                                    for (zz = 0; zz < c.ninv; zz++) {
//                                        if (temp == c.invlist[zz].release) {
//                                            redo = 1;
//                                            break;
//                                        }
//                                    }
//                                    if (redo == 1) {
//                                        continue;
//                                    }
//                                    break;
//                                } while (true); // not good, but dont really care...
//                                assert (c.sync_points[n_async].t > thisinv.release);
                        } else {
                            c.sync_points[n_async].t = -1;
                        }
                        n_async++;
                    }
                }
            }
        }
//        int sync = 0, async = 0;
//        for (i = 0; i < d - 1; i++) {                        
//            for (j = 0; j < validw[i]; j++) {
//                for (k = 0; k < g[i][j].invlist.length; k++) {
//                    if (g[i][j].invlist[k].async == 1) {
//                        async++;
//                    } else {
//                        sync++;
//                    }
//                }
//            }
//        }
//        System.out.println("async "+ async+" ,sync "+ sync+"..."+ ((double)async / (sync+async)));
        return 0;
    }

    int check_u(Task t, component c) {
        if (c.e > t.p) {
            return -1;
        }
        for (int i = 0; i < c.ninv; i++) {
            if (check_u(t, c.invlist[i].c) < 0) {
                return -1;
            }
        }
        return 0;
    }

    int mark(component c, int accessed[][]) {

        accessed[c.x][c.y] = 1;
        for (int i = 0; i < c.ninv; i++) {
            mark(c.invlist[i].c, accessed);
        }

        return 0;
    }

    int mark_dead_comp() {
        int accessed[][] = new int[config.depth][config.width];
        int i, j;
        for (i = 0; i < config.ntasks; i++) {
            if (task[i] == null) {
            }
            mark(task[i].home, accessed);
        }

        for (i = 0; i < config.depth; i++) {
            for (j = 0; j < config.width; j++) {
                if (g[i][j] == null) {
                    break;
                }
                if (accessed[i][j] == 0) {
                    g[i][j].dead = 1;
                }
            }
        }

        return 0;
    }

    int init_core() {
        core = new Core[config.ncpus];
        int i, j;
        for (i = 0; i < config.ncpus; i++) {
            core[i] = new Core(i);
            core[i].assigned_head = new Assign[config.ntasks];
            core[i].sum_assign = new double[config.ntasks];
            for (j = 0; j < config.ntasks; j++) {
                core[i].assigned_head[j] = new Assign();
            }
        }
        return 0;
    }

    long critical_path(Task t) {
        long rt = 0;
        long ret;

        init();

        assert (config.method == 6);
        calc_assign_chain(t);
        calc_with_parallel(t);
        int ncpu;
        Core[] cpu_rr;
        int max_cpu = config.ncpus;
        //int max_cpu = t.n_async + 1;
        ncpu = max_cpu;

        cpu_rr = new Core[ncpu];
        int[] tried = new int[config.ncpus];
        for (int j = 0; j < ncpu; j++) {
            cpu_rr[j] = core[j];
        }
        t.async_ncpus = ncpu;
        t.assigned = 1;
        ret = assign_subtask(t, t.n_async + 1, cpu_rr, ncpu);

        t.assigned = 0;
        if (ret < 0) {
            return -1;
        }
        assert (t.last_rt <= t.d);

        if (config.fork_join > 0) {
            component c = t.home;
            assert (c.e == 0);
            long sum = 0;
            for (int i = 0; i < c.ninv; i++) {
                sum += c.invlist[i].c.e;
            }
            if (config.cost_inv == 0) {
                assert (sum == t.last_rt);
            }
        }

        init();

        return 0;
    }

    int generate_tasks() {
        int i, j;
        component c;
        task = new Task[config.ntasks];
        Task t;
        int left = config.ntasks;
        double[] ulist = new double[config.ntasks];
        double u_tot;

        do {
            do {
                u_tot = 0;
                for (i = 0; i < config.ntasks - 1; i++) {
                    ulist[i] = this.get_rnd_even_double(config.u_max, config.u_min);
                    u_tot += ulist[i];
                }
            } while (u_tot >= config.totU);
            ulist[i] = config.totU - u_tot;
        } while (ulist[i] > config.u_max || ulist[i] < config.u_min);

//        u_tot=0;
//        for (i = 0; i < config.ntasks; i++) {
//            u_tot+=ulist[i];
//            System.out.print(ulist[i]+" ");
//        }
//        System.out.print(u_tot+" ");

        for (i = 0; i < d; i++) {
            for (j = 0; j < validw[i]; j++) {
                c = g[i][j];
                if (c.parents == 0) {
                    if (left > 0) {
                        t = new Task(c, ulist[config.ntasks - left]);
                        task[config.ntasks - left] = t;
                        left--;
                    } else {
                        //assert (i > 0);
                        c.dead = 1;
                        c.e = 0;
//                      System.out.println(".");
                    }
                }
            }
            if (config.fork_join > 0 || config.do_async > 0) {
                break;
            }
        }

        for (; left > 0; left--) {
            do {
                i = (int) this.get_rnd(config.home_level_mean, d, config.home_level_var) - 1;
            } while (i >= (config.depth / 2));
            //System.out.println(i);
            if (config.fork_join > 0 || config.do_async > 0) {
                i = 0;
            }
            j = (int) this.get_rnd_even((int) validw[i]);
            c = g[i][j];
            t = new Task(c, ulist[config.ntasks - left]);
            task[config.ntasks - left] = t;
        }

        mark_dead_comp();

        for (i = 0; i < config.ntasks - 1; i++) {

            for (j = i + 1; j < config.ntasks; j++) {
                if (task[i].p > task[j].p) {
                    Task temp;
                    temp = task[i];
                    task[i] = task[j];
                    task[j] = temp;
                }
            }
            //System.out.println(task[i].p + ", ");
        }
        //System.out.println(task[i].p + ", ");
        for (i = 0; i < config.ntasks; i++) {
            if (check_u(task[i], task[i].home) < 0) {
                return -1;
            }
            task[i].id = i;
        }

        for (i = 0; i < config.ntasks; i++) {
            if (config.do_fj > 0 || config.do_async > 0) {
                long cp = critical_path(task[i]);
                if (cp < 0) {
                    return -1;
                }
            }
        }
//        double sum = 0;
//        for (i = 0; i < config.ntasks; i++) {
//            System.out.println(task[i].u);
//            sum += task[i].u;
//        }
//        System.out.println("sum:" + sum);
        return 0;
    }

    Assign assign_chain(component c, Assign chain, Task t, int async_idx, int parent_async_idx) {

        chain.next = new Assign(c);
        //      System.out.println(c.x + "," + c.y);
        Assign tail = chain.next;
        tail.thd = t;
        t.assign_length++;

        t.comp_access_a[c.x][c.y]++;
        long n_access = t.comp_access_a[c.x][c.y];

        if (config.fork_join > 0) {
            n_access = 1;
        }

//        if (n_access % config.call_freq == 1) {
//            tail.call_dep = true;
//        } else {
//            tail.call_dep = false;
//        }

        tail.async_idx = async_idx;
        tail.parent_async_idx = parent_async_idx;
        assert (parent_async_idx <= async_idx);

        for (int i = 0; i < c.ninv && (n_access % config.call_freq == 1); i++) {
            if (c.invlist[i].async == 1) {
                int j = 0;
                do {
                    if (j > 0) {
                        assert (c.invlist[i].fj_nseg >= 2);
                    }
                    j++;
                    t.n_async++;
                    tail = assign_chain(c.invlist[i].c, tail, t, t.n_async, async_idx);
                } while (j < c.invlist[i].fj_nseg);

//                if (c.invlist[i].fj_nseg >= 2) {
//                    tail = assign_chain(c.invlist[i].c, tail, t, async_idx);
//                } // limit one of the segs is on the current core.

            } else {
                tail = assign_chain(c.invlist[i].c, tail, t, async_idx, parent_async_idx);
            }
        }
        return tail;
    }

    void calc_assign_chain(Task t) {
        t.assign_length = 0;
        t.assign_head = new Assign(null);
        t.comp_access_a = new long[config.depth][config.width];
        t.n_async = 0;
        assign_chain(t.home, t.assign_head, t, 0, 0);
        //System.out.println(task[i].assign_length);
        // init home assign
//        System.out.println("one thd");
    }

    Exe exec_chain(component c, Exe chain, boolean parent_xcore) {
        //      System.out.println(c.x + "," + c.y);
        Assign local_a = global_a;
        assert (local_a != null);
        long tmp = local_a.c.e;
        long e_inv;

//        assert (local_a.cpu != null);
        Exe tail = chain;

        local_a.thd.comp_access_e[c.x][c.y]++;
        long n_access = local_a.thd.comp_access_e[c.x][c.y];
        if (config.fork_join > 0) {
            n_access = 1;
        }

        int i;
        boolean x_core = false;
        inv exe_sync_call = null;
        for (i = 0; i < c.ninv && (n_access % config.call_freq == 1); i++) {
            if (config.FJ_nomain == 0) {
                assert (c.invlist[i].relative_r > 0);
            }
            tmp -= c.invlist[i].relative_r;
            global_a = global_a.next;
            e_inv = c.invlist[i].relative_r + config.cost_inv;
            if (parent_xcore && i == 0) {
                e_inv += config.cost_ipi_bottom_half; //receiving from parent cost
            }

            if (x_core == true) {
                assert (i > 0);
                if (config.do_async > 0 || config.do_fj > 0) {
                    e_inv += 0; // async inv. no return ipi immediately.
                } else {
                    e_inv += config.cost_ipi_bottom_half; // return receiving
                }
            }

            if (global_a.cpu.id != local_a.cpu.id) {
                e_inv += config.cost_ipi_sending; // call sending
                x_core = true;
            } else {
                x_core = false;
            }

            assert (tail != null);
            tail.next = new Exe(e_inv, local_a.cpu, local_a.c, c.invlist[i], local_a);
            if (exe_sync_call != null) {
                tail.next.sync_call = exe_sync_call;
                exe_sync_call = null;
            }

            tail = exec_chain(c.invlist[i].c, tail.next, x_core);
            for (int j = 1; j < c.invlist[i].fj_nseg; j++) {
                assert (c.invlist[i].fj_nseg >= 2);
                global_a = global_a.next;
                if (global_a.cpu.id != local_a.cpu.id) {
                    x_core = true;
                } else {
                    x_core = false;
                }
                tail.next = new Exe(0, local_a.cpu, local_a.c, c.invlist[i], local_a);
                if (x_core) {
                    tail.next.e_t += config.cost_ipi_sending;
                } else {
                    tail.next.e_t += config.cost_inv;
                }
                tail.next.fj_seg = 1;
                tail = exec_chain(c.invlist[i].c, tail.next, x_core);
                exe_sync_call = c.invlist[i];
            }
        }

        e_inv = c.e;
        if (parent_xcore) {
            if (config.do_async > 0 || config.do_fj > 0) {
                e_inv += config.cost_async_evt_trigger; //split-merge
            } else {
                e_inv += config.cost_ipi_sending; //return sending
            }
        }

        if (c.ninv > 0 && (n_access % config.call_freq == 1)) {
            if (x_core == true) {
                assert (i > 0);
                if (config.do_async > 0 || config.do_fj > 0) {
                    e_inv += 0; // async inv. no return ipi immediately.
                } else {
                    e_inv += config.cost_ipi_bottom_half; // return receiving
                }
            }
            if (config.FJ_nomain == 0) {
                assert (c.e - c.invlist[i - 1].release > 0);
            }
            e_inv -= c.invlist[i - 1].release;
            tail.next = new Exe(e_inv, local_a.cpu, local_a.c, null, local_a);
            if (exe_sync_call != null) {
                tail.next.sync_call = exe_sync_call;
                exe_sync_call = null;
            }
        } else {
            assert (c.e > 0);
            if (parent_xcore) {
                e_inv += config.cost_ipi_bottom_half; //receiving from parent cost
            }
            tail.next = new Exe(e_inv, local_a.cpu, local_a.c, null, local_a);
        }
        tail = tail.next;

        return tail;
    }

    void insert_merge_cost(Task t) {
        assert (config.do_async > 0 || config.do_fj > 0);

        Exe prev = t.exe_head.next;
        Exe e = prev.next;

        while (e != null) {
            if (config.do_async > 0) {
                if (e.call != null) {
                    assert (e.next != null);
                    assert (e.call.async == 1);
                    if (e.next.assign.cpu.id != e.assign.cpu.id && e.call.async_return == 1) {
                        int parent = e.assign.async_idx;
                        Exe start = e.next;

                        long remaining = 0;
                        Exe go = start;
                        while (go != null) {
                            if (go.assign.async_idx == parent) {
                                remaining += go.e_t;
                            }
                            go = go.next;
                        }
                        assert (e.call.sync_point.t <= 1
                                && e.call.sync_point.t > 0);
                        remaining *= e.call.sync_point.t;
                        //Exe go = start;
                        go = start;
                        while (true) {
                            assert (go != null);
                            if (go.assign.async_idx == parent) {
                                if (remaining <= go.e_t) {
                                    go.e_t += config.cost_async_evt_wait;
                                    go.e_t += config.cost_async_parent_up;
                                    break;
                                } else {
                                    remaining -= go.e_t;
                                }
                            }
                            go = go.next;
                        }

                    }
                }
            } else { // FJ
                //assert (e.call.async_return == 1);
                if (e.assign.async_idx == 0) {
                    if (e.next != null) {
                        assert (e.next.c.e > 0);
                        assert (prev.c.e > 0);
                        if (prev.c.e != e.next.c.e) {
                            e.e_t += config.cost_async_evt_wait;
                            e.e_t += config.cost_async_parent_up;
                        }
                    }
                }
            }
            prev = e;
            e = e.next;
        }


    }

    void calc_exec_chain(Task t) {

        t.exe_head = new Exe();
        t.comp_access_e = new long[config.depth][config.width];
        global_a = t.assign_head.next;

//        assert (global_a.cpu != null);

        exec_chain(t.home, t.exe_head, false);
        assert (global_a.next == null);

        if ((config.do_fj > 0 || config.do_async > 0) && config.cost_inv > 0) {
            insert_merge_cost(t); // split_merge!!!
        }

        // sanity check!!!!
        Exe e = t.exe_head.next;
        Assign a = t.assign_head.next;
        double sum_e = 0, sum_a = 0;
        while (e != null) {
            sum_e += e.e_t;
            e = e.next;
        }
        while (a != null) {
            sum_a += a.c.e;
            a = a.next;
        }
        if (config.cost_inv == 0) {
            assert (sum_a == sum_e);
        } else {
            assert (sum_a <= sum_e);
        }
        assert (sum_a == t.wcet);
    }

    Core find_max_cpu() {
        double max_u = 0;
        int id = 0;
        for (int i = 0; i < config.ncpus; i++) {
            if (1 - core[i].u > max_u) {
                max_u = 1 - core[i].u;
                id = i;
            }
        }

        assert (core[id].u < 1);
        return core[id];
    }

    Core worst_fit(double u, int tried[], int final_try) {
        double max_u = 0;
        int id = 0, yes = 0;
        for (int i = 0; i < config.ncpus; i++) {
            if (tried[i] == 1) {
                continue;
            }

            if ((1 - core[i].u > max_u) && (1 - core[i].u >= u)) {
                max_u = 1 - core[i].u;
                id = i;
                yes = 1;
            }
        }
        if (yes > 0) {
            if (final_try > 0) {
                for (int i = 0; i < config.ncpus; i++) {
                    tried[i] = 1;
                }
            }
            assert (core[id].u < 1);
            return core[id];
        } else {
            return null;
        }

    }

    Core best_fit(double u, int tried[]) {
        double max_u = Double.MAX_VALUE;
        int id = 0, yes = 0;
        for (int i = 0; i < config.ncpus; i++) {
            if (tried[i] == 1) {
                continue;
            }

            if ((1 - core[i].u < max_u) && (1 - core[i].u >= u)) {
                max_u = 1 - core[i].u;
                id = i;
                yes = 1;
            }
        }
        if (yes == 1) {
            assert (core[id].u < 1);
            return core[id];
        } else {
            return null;
        }

    }

    Core first_fit(double u, int tried[]) {

        for (int i = 0; i < config.ncpus; i++) {
            if (tried[i] == 1) {
                continue;
            }
            if (1 - core[i].u >= u) {
                return core[i];
            }
        }
        return null;
    }

    Task get_task_prio(int i) {
        return task[i];
    }

    Task get_low_task_prio(int i) {
        return task[config.ntasks - i - 1];
    }

    Task get_task_util(int n) {
        int i, max = 0, assigned = 0;
        double u = 0;
        for (i = 0; i < config.ntasks; i++) {
            if (task[i].assigned == 1) {
                assigned++;
                continue;
            }
            if (task[i].u > u) {
                u = task[i].u;
                max = i;
            }
        }

        assert (assigned == n);
        assert (u > 0);
        return task[max];
    }

    int calc_thd_core_access(Task t) {
        Exe e = t.exe_head.next;
        int temp[] = new int[config.ncpus];
        int temp_sum = 0;
        while (true) {
            if (e.next == null) {
                break;
            } else {
                if (e.cpu.id != e.next.cpu.id) {
                    temp[e.next.cpu.id]++;
                }
            }
            e = e.next;
        }
        int i;
        for (i = 0; i < config.ncpus; i++) {
            this.thd_core[t.id][i] = temp[i];
            temp_sum += temp[i];
        }
        assert (temp_sum == t.path.length - 1);
        if (config.fork_join == 0) {
            assert (temp_sum % 2 == 0);
        }
        return 0;
    }

    int analysis(Task new_t) {
        assert (new_t != null);

        if (new_t != null) {
            this.calc_exec_chain(new_t);
            this.calc_folds(new_t);
            this.calc_thd_core_access(new_t);
        }

        if (config.method == 0) {
            return RTA(new_t);
        } else if (config.method == 1) {
            return this.praveen(0, new_t);
        } else if (config.method == 2) {
            return this.praveen(1, new_t);
        } else if (config.method == 3) {
            return this.holistic(new_t);
        } else {
            return this.holistic_async(new_t);
        }
    }

    int cpu_alloc(Core cpu, Task curr_t, Assign start, long alloc_length) {
        Assign a = start;

        //cpu.u += curr_t.u;
        double sum = 0;
        long i = alloc_length;
        Assign curr, n;
        curr = core[cpu.id].assigned_head[curr_t.id];
        assert (curr != null);

        assert (curr.next == null);
        while (curr.next != null) {
            curr = curr.next;
        }
        while (i > 0) {
            assert (a != null);
            n = new Assign();
            a.cpu = cpu;
            sum += a.c.e;
            n.copy(a);
            n.thd = curr_t;
            n.next = null;
            n.orig = a;
            curr.next = n;
            curr = curr.next;
            assert (a.cpu != null);
            a = a.next;
            i--;
        }
        core[cpu.id].sum_assign[curr_t.id] += sum;
        cpu.u += sum / curr_t.p;
        assert (cpu.u <= 1.00000001);

        return 0;
    }

    int cpu_remove(Core cpu, Task curr_t, Assign from, long remove_length) {
        Assign a = from;
        //cpu.u += curr_t.u;
        double sum = 0;
        long i = remove_length;
        Assign curr;
        curr = core[cpu.id].assigned_head[curr_t.id];
        assert (curr != null);

        assert (curr.next != null);
        while (curr.next.orig != from) {
            curr = curr.next;
        }
        assert (curr.next.orig == from);
        curr.next = null;
        while (i > 0) {
            assert (a != null);
            assert (a.cpu != null);
            a.cpu = null;
            sum += a.c.e;
            a = a.next;
            i--;
        }
        core[cpu.id].sum_assign[curr_t.id] -= sum;
        cpu.u -= sum / curr_t.p;

        int j;
        for (j = 0; j < config.ntasks; j++) {
            if (core[cpu.id].sum_assign[j] > 0) {
                break;
            }
        }
        if (j == config.ntasks) {
            cpu.u = 0;
        }

        assert (cpu.u >= 0);

        return 0;
    }

    int cpu_alloc_async(Core cpu, Task curr_t, Async_assign start, long alloc_length) {
        Async_assign a = start;
        assert (a != null);
        //cpu.u += curr_t.u;
        double sum = 0;
        long i = alloc_length;
        Assign curr, n;
        curr = core[cpu.id].assigned_head[curr_t.id];
        assert (curr != null);

//        assert (curr.next == null);
        while (curr.next != null) {
            curr = curr.next;
        }
        while (i > 0) {
            assert (a != null);
            n = new Assign();
            a.a.cpu = cpu;
            sum += a.a.c.e;
            n.copy(a.a);
            n.thd = curr_t;
            n.next = null;
            n.orig = a.a;
            curr.next = n;
            curr = curr.next;
            assert (a.a.cpu != null);
            a = a.next;
            i--;
        }
        core[cpu.id].sum_assign[curr_t.id] += sum;
        cpu.u += sum / curr_t.p;
        assert (cpu.u <= 1.00000001);

        return 0;
    }

    int cpu_remove_async(Core cpu, Task curr_t, Async_assign from, long remove_length) {
        Async_assign a = from;
        //cpu.u += curr_t.u;
        double sum = 0;
        long i = remove_length;
        Assign curr;
        curr = core[cpu.id].assigned_head[curr_t.id];
        assert (curr != null);

        assert (curr.next != null);
        assert (from.a != null);
        while (curr.next.orig != from.a) {
            curr = curr.next;
            assert (curr.next != null);
        }
        assert (curr.next.orig == from.a);
        curr.next = null;
        while (i > 0) {
            assert (a != null);
            assert (a.a.cpu != null);
            a.a.cpu = null;
            sum += a.a.c.e;
            a = a.next;
            i--;
        }
        core[cpu.id].sum_assign[curr_t.id] -= sum;
        cpu.u -= sum / curr_t.p;

        int j;
        for (j = 0; j < config.ntasks; j++) {
            if (core[cpu.id].sum_assign[j] > 0) {
                break;
            }
        }
        if (j == config.ntasks) {
            cpu.u = 0;
        }

        assert (cpu.u >= 0);
        return 0;


    }

    class Cut {

        Assign a;
        long length;
        long cut_e;
        Async_assign async;

        public Cut(Assign a, long i, long ee) {
            this.a = a;
            length = i;
            cut_e = ee;
        }

        public Cut(Async_assign a, long i, long ee) {
            this.async = a;
            length = i;
            cut_e = ee;
        }
    }

    Cut cut_half(Assign from, long length) {
        Assign a;
//        a = curr_t.assign_head.next;
//        while (a != from) {
//            a = a.next;
//        }
        a = from;
        double sum = 0, partial = 0, min_diff, old_diff, middle;
        long i;
        for (i = 0; i < length; i++) {
            sum += a.c.e;
            a = a.next;
        }
        middle = sum / 2;
        a = from;

        old_diff = Long.MAX_VALUE;
        assert (length >= 2);
        for (i = 0; i < length; i++) {
            partial += a.c.e;
            min_diff = Math.abs(partial - middle);
            if (min_diff >= old_diff) {
                break;
            }
            old_diff = min_diff;
            a = a.next;
        }
        assert (i > 0);
        long cut_e = (long) partial - a.c.e;
        Cut ret = new Cut(a, i, cut_e);
        return ret;
    }

    Cut cut_half_async(Async_assign from, long length) {
        Async_assign a;
//        a = curr_t.assign_head.next;
//        while (a != from) {
//            a = a.next;
//        }
        a = from;
        double sum = 0, partial = 0, min_diff, old_diff, middle;
        long i;
        for (i = 0; i < length; i++) {
            sum += a.a.c.e;
            a = a.next;
        }
        middle = sum / 2;
        a = from;

        old_diff = Long.MAX_VALUE;
        assert (length >= 2);
        for (i = 0; i < length; i++) {
            partial += a.a.c.e;
            min_diff = Math.abs(partial - middle);
            if (min_diff >= old_diff) {
                break;
            }
            old_diff = min_diff;
            a = a.next;
        }
        assert (i > 0);
        long cut_e = (long) partial - a.a.c.e;
        Cut ret = new Cut(a, i, cut_e);
        return ret;
    }

    int calc_comp_u() {
        int i, j;
        for (i = 0; i < config.depth; i++) {
            for (j = 0; j < config.width; j++) {
                if (g[i][j] == null) {
                    break;
                }
                g[i][j].thd_u = new double[config.ntasks];
            }
        }
        for (i = 0; i < config.ntasks; i++) {
            Assign a = task[i].assign_head.next;
            assert (a != null);
            while (a != null) {
                a.c.thd_u[i] += a.c.e / task[i].p;
                a = a.next;
            }
        }
        double sum = 0;
        int k;
        for (i = 0; i < config.depth; i++) {
            for (j = 0; j < config.width; j++) {
                if (g[i][j] == null) {
                    break;
                }
                g[i][j].comp_u = 0;
                for (k = 0; k < config.ntasks; k++) {
                    g[i][j].comp_u += g[i][j].thd_u[k];
                }
                if (g[i][j].comp_u > 1) {
                    return -1;
                }
                sum += g[i][j].comp_u;
            }
        }
        assert (sum - config.totU < 0.000001);
        return 0;




    }

    class Sub_thd {

        double p, e, d, u;
        Sub_thd next;
        boolean sort = false;
        Task t;

        public Sub_thd(double p, double e, double d, Task t) {
            this.p = p;
            this.e = e;
            this.d = d;
            this.t = t;
            u = e / p;
        }

        public Sub_thd() {
        }
    }

    int calc_comp_subthds() {
        int i, j;
        for (i = 0; i < config.depth; i++) {
            for (j = 0; j < config.width; j++) {
                if (g[i][j] == null) {
                    break;
                }
                g[i][j].thds = new Sub_thd();
                g[i][j].end = g[i][j].thds;
            }
        } // init done;

        for (i = 0; i < config.ntasks; i++) {
            Exe e = task[i].exe_head.next;
            long sum = 0;
            assert (e != null);
            while (e != null) {
                e.c.end.next = new Sub_thd(task[i].p, e.e_t,
                        ((double) e.e_t / (double) task[i].wcet) * task[i].d, task[i]);
                e.c.end = e.c.end.next;
                sum += e.e_t;
                e = e.next;
            }
            assert (sum == task[i].wcet);
        }

        return 0;
    }

    component get_high_util_comp() {
        double u = 0;
        int i, j;
        component ret = null;
        for (i = 0; i < config.depth; i++) {
            for (j = 0; j < config.width; j++) {
                if (g[i][j] == null) {
                    break;
                }
                if (g[i][j].dead == 1) {
                    continue;
                }
                if (g[i][j].comp_u > u && g[i][j].assigned == false) {
                    ret = g[i][j];
                    u = ret.comp_u;
                }
            }
        }

        return ret;
    }

    Sub_thd get_next_thd(Core cpu) {
        Sub_thd ret = null;
        Assigned_comp ac;
        ac = cpu.ac.next;
        double low_d = Double.MAX_VALUE;
        assert (ac != null);
        while (ac != null) {
            Sub_thd t = ac.c.thds.next;
            assert (t != null);
            while (t != null) {
                if (t.sort == false) {
                    if (t.d < low_d) {
                        ret = t;
                        low_d = t.d;
                    }
                }
                t = t.next;
            }
            ac = ac.next;
        }

        ret.sort = true;
        return ret;
    }

    int analysis_core(Core cpu) {
        Assigned_comp ac;
        int i, j, ret = 0;
        ac = cpu.ac.next;
        assert (ac != null);
        int num_t = 0;
        while (ac != null) {
            Sub_thd t = ac.c.thds.next;
            assert (t != null);
            while (t != null) {
                num_t++;
                t = t.next;
            }
            ac = ac.next;
        }
        Sub_thd[] thds = new Sub_thd[num_t];
        for (i = 0; i < num_t; i++) {
            thds[i] = get_next_thd(cpu);
            //System.out.println(thds[i].d);
        }
        double r1, r0;
        boolean end = false;
        for (i = 0; i < num_t && end != true; i++) {
            r0 = 0;
            while (true) {
                r1 = thds[i].e;
                if (thds[i].t.u > 1) {
                    j = i;
                } else {
                    j = i - 1;
                }
                for (; j >= 0; j--) {
                    if (thds[j].t.id == thds[i].t.id && thds[i].t.u <= 1) {
                        assert (thds[j].t == thds[i].t);
                        continue;
                    }
                    assert (thds[j].p == thds[j].t.p);
                    r1 += Math.ceil(r0 / thds[j].p) * thds[j].e;
                }
                if (r1 == r0) {
                    break;
                }
                if (r1 > thds[i].d) {
                    ret = -1;
                    end = true;
                    break;
                }
                r0 = r1;
            }
        }
        for (i = 0; i < num_t; i++) {
            thds[i].sort = false;
        }
        return ret;
    }

    component pre_order_unassigned(component root) {
        component ret;
        if (root.assigned == false) {
            return root;
        } else {
            int i;
            for (i = 0; i < root.ninv; i++) {
                ret = pre_order_unassigned(root.invlist[i].c);
                if (ret != null) {
                    return ret;
                } else {
                    continue;
                }
            }
        }
        return null;
    }

    component next_unassigned() {
        int i, j;
        component ret;
        for (i = 0; i < config.depth; i++) {
            for (j = 0; j < config.width; j++) {
                if (g[i][j] == null) {
                    break;
                }
                if (g[i][j].dead == 1) {
                    continue;
                }

                ret = pre_order_unassigned(g[i][j]);
                if (ret != null) {
                    return ret;
                }
            }
        }
        for (i = 0; i < config.depth; i++) {
            for (j = 0; j < config.width; j++) {
                if (g[i][j] == null) {
                    break;
                }
                if (g[i][j].dead == 1) {
                    continue;
                }
                assert (g[i][j].assigned == true);
            }
        }
        return null;
    }

    int assign_comp() {
        int i, j, ret;
        for (i = 0; i < config.ntasks; i++) {
            this.calc_exec_chain(task[i]);
        }
        ret = calc_comp_u();

        if (ret < 0) {
            return ret;
        }
        //if (ret == 0) return 0;
        calc_comp_subthds();
        for (i = 0; i < config.ncpus; i++) {
            core[i].ac = new Assigned_comp();
        }

        for (i = 0; i < config.depth; i++) {
            for (j = 0; j < config.width; j++) {
                if (g[i][j] == null) {
                    break;
                }
                if (g[i][j].dead == 1) {
                    assert (g[i][j].comp_u == 0);
                    continue;
                }
                g[i][j].assigned = false;
            }
        }
        component c;

        while (true) {
            //c = get_high_util_comp();
            c = next_unassigned();
            if (c == null) {
                break;
            }
            assert (c.dead == 0);
            assert (c.assigned == false);
            assert (c.comp_u > 0);

            c.assigned = true;
            int tried[] = new int[config.ncpus];
            while (true) {
                Core cpu = this.first_fit(c.comp_u, tried);
                if (cpu == null) {
                    return -1;
                }
                tried[cpu.id] = 1;
                cpu.u += c.comp_u;

                Assigned_comp ac;
                ac = cpu.ac;
                while (ac.next != null) {
                    ac = ac.next;
                }
                ac.next = new Assigned_comp();
                ac = ac.next;
                ac.c = c;

                ret = analysis_core(cpu);
                if (ret == 0) {
                    break;
                }

                if (ret < 0) {
                    cpu.u -= c.comp_u;
                    ac = cpu.ac;
                    while (ac.next.c != c) {
                        ac = ac.next;
                    }
                    assert (ac.next.next == null);
                    ac.next = null;
                }
                //System.out.println(c.comp_u);
            }
        }

        for (i = 0; i < config.depth; i++) {
            for (j = 0; j < config.width; j++) {
                if (g[i][j] == null) {
                    break;
                }
                if (g[i][j].dead == 1) {
                    assert (g[i][j].comp_u == 0);
                    continue;
                }
                assert (g[i][j].assigned == true);
            }
        }
        return 0;
    }

    int assign() {
        int i, j, ntasks = config.ntasks;
        int ret;
        Core cpu;

        for (i = 0; i < ntasks; i++) {
            //Task curr_t = get_task_prio(i);
            Task curr_t;
            if (config.task_order == 0) { // HUF
                curr_t = get_task_util(i);
            } else if (config.task_order == 1) {
                curr_t = get_low_task_prio(i);
            } else {
                curr_t = task[i]; // high prio
            }

            //decide if schedulable 

            curr_t.assigned = 2; //assigning!

            int success = 0, tried[] = new int[config.ncpus];
            while (success == 0) {
                cpu = best_fit(curr_t.u, tried);
                //cpu = worst_fit(curr_t.u, tried);

                if (cpu != null) {
                    tried[cpu.id] = 1;

                    cpu_alloc(cpu, curr_t, curr_t.assign_head.next, curr_t.assign_length);
                    ret = this.analysis(curr_t);
                    if (ret == 0) {
                        success = 1;
                        curr_t.assigned = 1;
                    } else {
                        cpu_remove(cpu, curr_t, curr_t.assign_head.next, curr_t.assign_length);
                        //undo above....sigh....
                    }
                    //            System.out.println(i + "->" + cpu.id);
                } else {
                    if (config.split == 0) {
                        return -1;
                    }
                    // trying split!!!
                    //System.out.println("splitting!\n");
                    int ncut = 1, nn;

                    long old_length, old_e;
                    Cut n[] = new Cut[ncut], old_n[];
                    n[0] = new Cut(curr_t.assign_head.next, curr_t.assign_length, curr_t.wcet);

                    while (success == 0) {
                        ncut *= 2;
                        if (ncut > config.ncpus || (ncut / 2 > curr_t.assign_length)) {
                            //System.out.println("failed...\n");
                            return -1;
                        }

                        Cut cut_ret;
                        old_n = n;
                        n = new Cut[ncut];
                        for (int m = 0; m < ncut; m += 2) {
                            if (old_n[m / 2] == null) {
                                n[m] = null;
                                n[m + 1] = null;
                                continue;
                            }
                            if (old_n[m / 2].length >= 2) {
                                cut_ret = cut_half(old_n[m / 2].a, old_n[m / 2].length);
                                n[m] = old_n[m / 2];
                                old_length = n[m].length;
                                old_e = n[m].cut_e;
                                n[m].length = cut_ret.length;
                                n[m].cut_e = cut_ret.cut_e;
                                assert (old_length > n[m].length);
                                n[m + 1] = new Cut(cut_ret.a, old_length - n[m].length, old_e - n[m].cut_e);
                            } else {
                                n[m] = old_n[m / 2];
                                n[m + 1] = null;
                            }
                        }

                        // all cuts in n[]                        

                        tried = new int[config.ncpus];
                        while (true) {
                            Core cpus[] = new Core[ncut];
                            int fit_fail = 0;
                            for (int m = 0; m < ncut; m++) {
                                if (n[m] == null) {
                                    continue;
                                }
                                cpu = best_fit(n[m].cut_e / curr_t.p, tried);
                                if (cpu == null) {
                                    fit_fail = 1;
                                    break;
                                }
                                tried[cpu.id] = 1;
                                cpus[m] = cpu;
                            }
                            if (fit_fail == 1) {
                                break;
                            }

                            // found cpus!
                            for (int m = 0; m < ncut; m++) {
                                if (n[m] == null) {
                                    continue;
                                }
                                cpu_alloc(cpus[m], curr_t, n[m].a, n[m].length);
                            }
                            ret = this.analysis(curr_t);
                            if (ret == 0) {
                                success = 1;
                                curr_t.assigned = 1;
                                break;
                                //System.out.println("Passsssssssssss");
                            } else {
                                for (int m = 0; m < ncut; m++) {
                                    if (n[m] == null) {
                                        continue;
                                    }
                                    cpu_remove(cpus[m], curr_t, n[m].a, n[m].length);
                                }
                                //undo above....sigh....
                            }
                        }
                    } // end of while success==0
                }
            } // end of split

        } // end of tasks assign loop
//        double sum = 0;
        for (i = 0; i < config.ntasks; i++) {
            assert (task[i].assigned == 1);

        }
//        System.out.println(sum + "///////////");
        return 0;
    }

    int naive_assign_subtask(Task t) {
        int ret;

        Task subt;
        int subt_idx = t.n_async + 1;
        Core cpu[] = new Core[subt_idx];
        int i, fail = 0, success = 0, assigned = 0;

        for (i = 0; i < subt_idx; i++) {
            subt = t.async_subt[i];
            int[] tried = new int[config.ncpus];
            //cpu[i] = rr_cpu[i % ncpu];//worst_fit(subt.u, tried, 1);
            cpu[i] = worst_fit(subt.u, tried, 0);

            if (cpu[i] != null) {
                assert (1 - cpu[i].u >= subt.u);
                cpu_alloc_async(cpu[i], subt, subt.async_chain.next, subt.assign_length);
                //            System.out.println(i + "->" + cpu.id);
            } else {
                fail = 1;
                break;
            }
        }

        assigned = i;
        if (fail == 0) {
            ret = this.analysis(t);
        } else {
            ret = -1;
        }

        if (ret == 0) {
            success = 1;
        } else {
            for (i = assigned - 1; i >= 0; i--) {
                assert (cpu[i] != null);
                subt = t.async_subt[i];
                cpu_remove_async(cpu[i], subt, subt.async_chain.next, subt.assign_length);
            }
            //cpu_remove_async(cpu, subt, subt.async_chain.next, subt.assign_length);
            //undo above....sigh....
            return -1;
        }

        assert (i == subt_idx);
        return 0;
    }

    int assign_subtask(Task t, int subt_idx, Core[] rr_cpu, int ncpu) {
        int ret;

        Task subt;
        Core cpu[] = new Core[subt_idx];
        int i, fail = 0, success = 0, tried[] = new int[config.ncpus], assigned = 0;
        for (i = 0; i < subt_idx; i++) {
            subt = t.async_subt[i];

            cpu[i] = rr_cpu[i % ncpu];//worst_fit(subt.u, tried, 1);

            if ((1 - cpu[i].u) >= subt.u) {
                cpu_alloc_async(cpu[i], subt, subt.async_chain.next, subt.assign_length);
                //            System.out.println(i + "->" + cpu.id);
            } else {
                fail = 1;
                break;
            }
        }

        assigned = i;
        if (fail == 0) {
            ret = this.analysis(t);
        } else {
            ret = -1;
        }

        if (ret == 0) {
            success = 1;
        } else {
            for (i = assigned - 1; i >= 0; i--) {
                assert (cpu[i] != null);
                subt = t.async_subt[i];
                cpu_remove_async(cpu[i], subt, subt.async_chain.next, subt.assign_length);
            }
            //cpu_remove_async(cpu, subt, subt.async_chain.next, subt.assign_length);
            //undo above....sigh....
            return -1;
        }

        assert (i == subt_idx);
        return 0;
    }

    int calc_with_parallel(Task curr_t) {
        Assign aa = curr_t.assign_head.next;
        curr_t.async_subt = new Task[curr_t.n_async + 1];
        Task subt;
        while (aa != null) {
            if (curr_t.async_subt[aa.async_idx] == null) {
                curr_t.async_subt[aa.async_idx] = new Task();
                subt = curr_t.async_subt[aa.async_idx];
                subt.id = curr_t.id;
                subt.assign_head = new Assign();
                subt.assign_head.next = aa;
                subt.home = aa.c;
                subt.assign_length = 0;
                subt.async_chain = new Async_assign();
                subt.async_tail = subt.async_chain;
                subt.async_parent = new Task[curr_t.n_async + 1];
                int p = 0;
                Assign pa = aa;
                subt.async_parent[0] = subt;
                if (curr_t.n_async > 0) {
                    do {
                        p++;
                        assert (pa.parent_async_idx <= aa.async_idx);
                        subt.async_parent[p] = curr_t.async_subt[pa.parent_async_idx];
                        if (pa.parent_async_idx == 0) {
                            break;
                        }
                        pa = subt.async_parent[p].assign_head.next;
                    } while (true);
                    Task temp;
                    for (int j = 0; j <= p / 2; j++) { // p could be 1
                        temp = subt.async_parent[j];
                        subt.async_parent[j] = subt.async_parent[p - j];
                        subt.async_parent[p - j] = temp;
                    } // parent sequence done.
                }
            }
            subt = curr_t.async_subt[aa.async_idx];
            subt.add_async(aa);
            subt.assign_length++;
            aa = aa.next;
        }
        int ii;
        long sum = 0;
        for (ii = 0; ii < curr_t.n_async + 1; ii++) {
            subt = curr_t.async_subt[ii];
            Async_assign asynca = subt.async_chain.next;
            while (asynca != null) {
                subt.wcet += asynca.a.c.e;
                asynca = asynca.next;
            }
            subt.p = curr_t.p;
            subt.d = curr_t.d;
            subt.u = subt.wcet / subt.p;
            sum += subt.wcet;
        }
        assert (sum == curr_t.wcet);
        return 0;
    }

    int calc_with_no_parallel(Task curr_t) {
        Assign aa = curr_t.assign_head.next;
        curr_t.async_subt = new Task[1];
        Task subt;

        curr_t.async_subt[0] = new Task();
        subt = curr_t.async_subt[0];
        subt.id = curr_t.id;
        subt.assign_head = new Assign();
        subt.assign_head.next = aa;
        subt.home = aa.c;
        subt.assign_length = 0;
        subt.async_chain = new Async_assign();
        subt.async_tail = subt.async_chain;

        while (aa != null) {
            subt.add_async(aa);
            subt.assign_length++;
            aa = aa.next;
        }

        Async_assign asynca = subt.async_chain.next;
        while (asynca != null) {
            subt.wcet += asynca.a.c.e;
            asynca = asynca.next;
        }
        subt.p = curr_t.p;
        assert (curr_t.async_subt[0].p == curr_t.p);
        subt.d = curr_t.d;
        assert (curr_t.async_subt[0].d == curr_t.d);
        subt.u = curr_t.u;

        assert (curr_t.async_subt[0].wcet == curr_t.wcet);
        return 0;
    }

    int assign_async() {
        int i, ntasks = config.ntasks;

        for (i = 0; i < ntasks; i++) {
            Task curr_t;

            if (config.task_order == 0) { // HUF
                curr_t = get_task_util(i);
            } else if (config.task_order == 1) {
                curr_t = get_low_task_prio(i);
            } else {
                curr_t = task[i]; // high prio
            }
            //System.out.println(curr_t.n_async);
            //decide if schedulable 

            curr_t.assigned = 2; //assigning!

            int ret = 0;

            calc_with_parallel(curr_t);
            if (config.async_naive_assign > 0) {
                ret = this.naive_assign_subtask(curr_t);
            } else {
                // target core round robin!!!
                int ncpu;
                Core[] cpu_rr;
                int max_cpu;
                max_cpu = config.ncpus;
//            if ((int) Math.ceil(curr_t.u) * 4 > config.ncpus) {
//                max_cpu = config.ncpus;
//            } else {
//                max_cpu = (int) Math.ceil(curr_t.u) * 4;
//            }
                if (config.async_only_partiton > 0) {
                    ncpu = 1;
                } else {
                    ncpu = (int) Math.ceil(curr_t.u);
                }
                //ncpu = max_cpu;

                for (; ncpu <= max_cpu; ncpu++) {
                    cpu_rr = new Core[ncpu];
                    int[] tried = new int[config.ncpus];
                    for (int j = 0; j < ncpu; j++) {
                        cpu_rr[j] = worst_fit(0, tried, 0);
                        tried[cpu_rr[j].id] = 1;
                    }
                    curr_t.async_ncpus = ncpu;

                    ret = assign_subtask(curr_t, curr_t.n_async + 1, cpu_rr, ncpu);
                    if (ret >= 0) {
                        //if (ncpu > 1) System.out.print("#");
                        break;
                    }
                    if (config.async_only_partiton > 0) {
                        break;
                    }
                }
            }
            if (ret < 0) {
                assert (i > 0);
                return -1;
            }

            curr_t.assigned = 1;
            if (config.async_only_partiton > 0) {
                assert (curr_t.async_ncpus == 1);
            }
        } // end of tasks assign loop

        for (i = 0; i < config.ntasks; i++) {
            assert (task[i].assigned == 1);
        }
//        System.out.println(sum + "///////////");
        return 0;
    }

    int naive_assign() {
        int i, j, ntasks = config.ntasks;
        this.core = new Core[config.ncpus];

        Core cpu;
        for (i = 0; i < config.ncpus; i++) {
            core[i] = new Core(i);
            core[i].assigned_head = new Assign[config.ntasks];
            for (j = 0; j < ntasks; j++) {
                core[i].assigned_head[j] = new Assign();
            }
        }

        for (i = 0; i < ntasks; i++) {
            task[i].fold_init();
            task[i].assigned = 0;
        }
        // init done.

        for (i = 0; i < ntasks; i++) {
            Task curr_t = get_low_task_prio(i);
            //Task curr_t = get_task_util(i);

            cpu = best_fit(curr_t.u, null);
            //cpu = worst_fit(curr_t.u, null);
            //decide if schedulable 

            Assign a = curr_t.assign_head.next;
            curr_t.assigned = 1;

            if (cpu != null) {
                //simple!
                cpu.u += curr_t.u;

                //f.next = null;
                Assign curr, n;
                curr = core[cpu.id].assigned_head[curr_t.id];
                assert (curr != null);

                assert (curr.next == null);
                while (curr.next != null) {
                    curr = curr.next;
                }
                while (a != null) {
                    n = new Assign();
                    a.cpu = cpu;
                    n.copy(a);
                    n.thd = curr_t;
                    n.next = null;
                    curr.next = n;
                    curr = curr.next;
                    assert (a.cpu != null);
                    a = a.next;
                }
                //            System.out.println(i + "->" + cpu.id);
            } else {
                cpu = find_max_cpu();
                // split!
                //System.out.print(".");
                if (config.split == 0) {
                    //System.out.println("no split");
                    return -1;
                }
                Assign curr, n;
                assert (a != null);
                while (a != null) {
                    curr = core[cpu.id].assigned_head[curr_t.id];

                    assert (curr != null);
                    while (curr.next != null) {
                        curr = curr.next;
                    }
                    while (a != null && a.c.e / curr_t.p < (1 - cpu.u)) {
                        cpu.u += a.c.e / curr_t.p;
                        assert (cpu.u <= 1);
                        n = new Assign();

                        a.cpu = cpu;
                        n.copy(a);
                        n.thd = curr_t;
                        n.next = null;
                        curr.next = n;
                        curr = curr.next;
                        assert (a.cpu != null);
                        a = a.next;
                    }

                    //          if (old_a == a) {
                    //            System.out.println("xxxx");
                    //      }
                    Core old_cpu = cpu;
                    cpu = this.find_max_cpu();
                    if (old_cpu == cpu) {
                        return -1;
                    }
                }
                //System.out.println("end task split: " + i);
            } // end of split
        } // end of tasks assign loop
//        double sum = 0;
//        for (i = 0; i < config.ncpus; i++) {
        //          System.out.println(core[i].u);
//            sum += core[i].u;
        //      }
//        System.out.println(sum + "///////////");

        return 0;
    }

    int holistic(Task new_t) {
        int i, j, k;
        // only need to redo for current and lower threads!
        //i = new_t.id;
        i = 0;
        for (; i < config.ntasks; i++) {
            assert (task[i].wcet == Math.round((task[i].p * task[i].u)));
            if (task[i].assigned == 0) {
                continue;
            }

            int l = task[i].path.length;
            double r[] = new double[l], r1;
            double jitter[] = new double[l];
            for (k = 0; k < l; k++) {
                r[k] = 0;
                jitter[k] = 0;
            }

            Core_node cn;
            cn = task[i].path.stage_list.next;
            assert (cn != null);
            int cid;
            for (k = 0; k < l; k++, cn = cn.next) {
                cid = cn.c.id;
                // iterate stage:

                int q = 0;
                while (true) {
                    // iterate q

                    while (true) {
                        r1 = cn.e * (q + 1);
                        assert (core[cid].assigned_head[i].next != null);

                        //if ((task[i].d > task[i].p) && (task[i].fold_list.next.length > 1) && (r[k] > task[i].p)) {
                        if ((task[i].d > task[i].p) && (task[i].fold_list.next.length > 1)) {
                            Core_node nn = task[i].path.stage_list.next;
                            assert (nn != null);
                            int mm = 0;
                            while (nn != null) {
                                if (nn == cn) {
                                    break;
                                }
                                if (nn.c.id == cid) {
                                    r1 += (long) (Math.ceil((r[k] + nn.jitter) / task[i].p) * nn.e);
                                }
                                if (mm == 0) {
                                    assert (nn.jitter == 0);
                                } else {
                                    assert (nn.jitter > 0);
                                }
                                nn = nn.next;
                                mm++;
                            }
                        }//self interference

                        for (j = i - 1; j >= 0; j--) {
                            if (core[cid].assigned_head[j].next == null) {
                                continue;
                            }

                            if (task[j].path.length > 1) {
                                assert (task[j].path.stage_list.next.next.jitter > 0);
                            }
                            //double e = core[cid].sum_assign[j];
                            Core_node nn = task[j].path.stage_list.next;
                            assert (nn != null);
                            while (nn != null) {
                                if (task[j].path.length == 1) {
                                    assert (nn.jitter == 0);
                                }
                                if (nn.c.id == cid) {
                                    r1 += (long) (Math.ceil((r[k] + nn.jitter) / task[j].p) * nn.e);
                                }
                                if (nn.next != null) {
                                    assert (nn.next.jitter > nn.jitter);
                                }
                                nn = nn.next;
                            }
                        }

                        //interference from IPIs!
                        long ipi_sum = 0;
                        assert (task[i].alloc_core[cid] == 1);
                        int jj;
                        for (jj = 0; jj < config.ntasks; jj++) {
                            if (task[jj].assigned == 0) {
                                continue;
                            }
                            long e = this.thd_core[jj][cid] * config.cost_ipi_top_half;
                            ipi_sum += e * Math.ceil(r[k] / task[jj].p);
                        }
                        //done IPI interference.
                        r1 += ipi_sum;

                        double tmp = r1 + jitter[k] - q * task[i].p;
                        if (tmp > task[i].d) {
                            return -1;
                        }

                        if (tmp > cn.max_r) {
                            cn.max_r = tmp;
                        }
                        if (r1 == r[k]) {
                            break;
                        }
                        r[k] = r1;
                    }
                    if (r1 <= (q + 1) * task[i].p) {
                        if (q > 1) {
                            //System.out.println(q);
                            assert (task[i].d > task[i].p);
                        }
                        if (config.u_max <= 1) {
                            assert (q == 0);
                        }
                        break;
                    }
                    assert (task[i].d > task[i].p);
                    assert (task[i].u > 1);
                    q++;
                } // for all q
                if (k + 1 < l) {
                    jitter[k + 1] = cn.max_r;
                    cn.next.jitter = cn.max_r;
                    assert (jitter[k + 1] > 0);
                    assert (jitter[k + 1] >= jitter[k]);
                }
            }//for all k -> stages

        } // for all tasks

        return 0;


    }

    class Async_call {

        inv call;
        int called;
        int synced;
        int parent_idx;
        Task t;
        long sync_remain;
        double release_t;
        double v_release_t;
        double rt;
        component c;
    }

    int checkout_sibling_if(Task t, Exe exe) {
        t.async_subt[exe.assign.async_idx].sibling_if_taken = 1;
        return 0;
    }

    double do_RTA(Task t, double e, double jitter, Core cpu, Exe exe, Exe last_exe) {
        double r1, r0 = 0;
        assert (t.d == t.p);
        if (e == 0) { // when we have FJ.
            return jitter;
        }
        assert (cpu.id == exe.cpu.id);
        assert (last_exe != null);
        Exe start_exe = last_exe;
        //self interference
        long self = 0;
        if (t.async_subt[exe.assign.async_idx].sibling_if_taken == 0) {
            if (config.fork_join == 0) {
                int curr = exe.assign.async_idx;
                // siblings
                for (int i = 1; i < t.n_async + 1; i++) {
                    if (i == curr) {
                        continue;
                    }
                    if (t.async_subt[i].assign_head.next.cpu.id != exe.cpu.id) {
                        continue;
                    }
                    int k = 0;
                    while (t.async_subt[i].async_parent[k]
                            == t.async_subt[curr].async_parent[k]) {
                        k++;
                    }
                    assert (k > 0);
                    if (t.async_subt[i].async_parent[k] == null
                            || t.async_subt[curr].async_parent[k] == null) {
                        continue;
                    }
                    assert (t.async_subt[i].async_parent[k - 1]
                            == t.async_subt[curr].async_parent[k - 1]);//k-1 is the closest shared parent
                    assert (t.async_subt[curr].assign_head.next.cpu.id == exe.cpu.id);

                    //some optimization. 
//                    int p = k - 1;
//                    boolean taken = false;
//                    while (t.async_subt[curr].async_parent[p] != t.async_subt[curr]) {
//                        if (t.async_subt[curr].async_parent[p].assign_head.next.cpu.id == exe.cpu.id) {
//                            taken = true;
//                            break;
//                        }
//                        p++;
//                    }
//                    if (taken) {
//                        continue;
//                    }

                    boolean yes = false;

                    if (t.async_subt[i].async_merge == Long.MAX_VALUE
                            && t.async_subt[curr].async_merge < Long.MAX_VALUE) {
                        yes = false;
                    } else {
                        if (t.async_subt[i].async_parent[k].async_merge
                                > t.async_subt[curr].async_parent[k].async_release) {
                            if (t.async_subt[i].async_parent[k].async_merge
                                    < t.async_subt[curr].async_parent[k].async_merge) {
                                yes = true;
                            } else if ((t.async_subt[i].async_parent[k].async_merge
                                    == t.async_subt[curr].async_parent[k].async_merge)
                                    && i < curr) {
                                yes = true;
                            }
                        }
                    }

                    if (yes) {
                        Exe self_if = t.async_subt[i].exe_head;
                        assert (self_if.cpu.id == exe.cpu.id);
                        while (self_if != null) {
                            if (self_if.assign.async_idx == i) {
                                self += self_if.e_t;
                            }
                            self_if = self_if.next;
                        }
                    }
                }
            } else {
                //FJ
                int curr = exe.assign.async_idx;
                inv call = t.async_subt[exe.assign.async_idx].async_call;
                for (int i = 1; i < t.n_async + 1; i++) {
                    if (i == curr) {
                        continue;
                    }
                    if (t.async_subt[i].async_call == call
                            && i < exe.assign.async_idx
                            && t.async_subt[i].assign_head.next.cpu.id == cpu.id) {
                        Exe go = t.async_subt[i].exe_head;
                        assert (go != null);
                        self += go.e_t;
                        if (exe.next != null) {
                            assert (exe.next.assign.async_idx != i);
                        }
                    }
                }
            }

        } // sibling if done

        // children if
        if (config.fork_join == 0) {
            int curr = exe.assign.async_idx;
            for (int i = 1; i < t.n_async + 1; i++) {
                if (i == curr) {
                    continue;
                }
                if (t.async_subt[i].exe_head.cpu.id != exe.cpu.id) {
                    continue;
                }
                int k = 0;
                while (t.async_subt[i].async_parent[k] != t.async_subt[curr]
                        && t.async_subt[i].async_parent[k] != t.async_subt[i]) {
                    k++;
                }
                assert (t.async_subt[i].async_parent[k] != null);
                if (t.async_subt[i].async_parent[k] == t.async_subt[curr]) {
                    assert (t.async_subt[i].assign_head.next.cpu.id
                            == t.async_subt[curr].assign_head.next.cpu.id);
                    boolean yes = false;
                    while (t.async_subt[i].async_parent[k] != t.async_subt[i]) {
                        if (t.async_subt[i].async_parent[k].exe_head.cpu.id != exe.cpu.id) {
                            yes = true;
                            break;
                        }
                        k++;
                    }
                    Exe go = start_exe;
                    if (yes) {
                        boolean yes_yes = false;
                        while (go != exe) {
                            if (t.async_subt[i].exe_head == go) {
                                yes_yes = true;
                                break;
                            }
                            go = go.next;
                            assert (go != null);
                        }
                        if (yes_yes) {
                            // found children interference!!
                            go = t.async_subt[i].exe_head;
                            assert (go.assign.cpu.id == exe.assign.cpu.id);//no cutting
                            while (go != null) {
                                if (go.assign.async_idx == i) {
                                    self += go.e_t;
                                }
                                go = go.next;
                            }
                        }
                    }
                }
            }
        }
        //self interference done

        while (true) {
            // no d > p anymore!!! Hooray!!
            //r1 = cn.e * (q + 1);
            r1 = e + self;

            //if ((task[i].d > task[i].p) && (task[i].fold_list.next.length > 1) && (r[k] > task[i].p)) {
            //if ((t.d > t.p) && (task[i].fold_list.next.length > 1)) {
//            if ((t.d > t.p) || (t.u > 1)) {
//                assert (false); // fix me
//                assert(t.fold_list.next.length > 1);
//                Core_node nn = t.path.stage_list.next;
//                assert (nn != null);
//                int mm = 0;
//                while (nn != null) {
//                    if (nn == cn) {
//                        break;
//                    }
//                    if (nn.c.id == cid) {
//                        r1 += (long) (Math.ceil((r[k] + nn.jitter) / task[i].p) * nn.e);
//                    }
//                    if (mm == 0) {
//                        assert (nn.jitter == 0);
//                    } else {
//                        assert (nn.jitter > 0);
//                    }
//                    nn = nn.next;
//                    mm++;
//                }
//            }//self interference
            int j;
            assert (exe != null);
            assert (exe.assign != null);

//            Exe go = exe;
//            while (go != null) {
//                assert (go.assign.cpu == go.cpu);
//                if (go.assign.async_idx < exe.assign.async_idx && go.cpu.id == exe.cpu.id) {
//                    r1 += go.e_t;
//                    //System.out.println("self-i");
//                }
//                go = go.next;
//            }


            for (j = t.id - 1; j >= 0; j--) {
                if (core[cpu.id].assigned_head[j].next == null) {
                    continue;
                }

                //double e = core[cid].sum_assign[j];
                Core_node nn = task[j].path.stage_list.next;
                assert (nn != null);
                while (nn != null) {
                    if (task[j].path.length == 1) {
                        assert (nn.jitter == 0);
                    }
                    if (nn.c.id == cpu.id && nn.e > 0) {
                        r1 += (long) (Math.ceil((r0 + nn.jitter) / task[j].p) * nn.e);
                    }
                    if (nn.next != null) {
                        //fix me
                        //assert (nn.next.jitter > nn.jitter);
                    }
                    nn = nn.next;
                }
            }


            //interference from IPIs!
            long ipi_sum = 0;
            assert (t.alloc_core[cpu.id] == 1);
            int jj;
            if (exe.assign.async_idx > 0) {
                for (jj = 0; jj < config.ntasks; jj++) {
                    if (task[jj].assigned == 0) {
                        continue;
                    }
                    if (jj == t.id) {
                        continue; // IPIs from itself included in cost_evt_up
                    }
                    long ipi_e = this.thd_core[jj][cpu.id] * config.cost_ipi_top_half;
                    ipi_sum += ipi_e * Math.ceil(r0 / task[jj].p);
                }
            }

            //done IPI interference.
            r1 += ipi_sum;

            double tmp = r1 + jitter;// - q * task[i].p;
            if (r1 + jitter > t.last_rt) {
                t.last_rt = (long) (r1 + jitter);
            }
            if (tmp > t.d) {
                return -1;
            }

//            if (tmp > cn.max_r) {
//                cn.max_r = tmp;
//            }
            if (r1 == r0) {
                break;
            }
            r0 = r1;
        }
        assert (r1 + jitter > 0);
        return r1 + jitter;
    }

    double async_RT(Task t, Exe start_exe, double jitter, component end) {

        Exe exe = start_exe;
        Exe prev = exe;
        int k = 0;
        long curr_e = 0;
        Exe last_exe = start_exe;
        int idx = exe.assign.async_idx;

        int l = t.path.length;
        //double r[] = new double[l], r1;
        double j[] = new double[l];
        j[0] = jitter; // initial jitter from parent
        long c_progress[][] = new long[config.depth][config.width];

        Async_call calls[] = new Async_call[t.n_async];
        int async_called = 0, async_synced = 0;

        while (true) {
            assert (exe != null);
            assert (prev != null);

            boolean acall = false;
            if (exe.cpu.id != prev.cpu.id) {
                acall = true;
            }
            if (config.fork_join > 0
                    && ((exe.assign.async_idx != prev.assign.async_idx) && t.async_ncpus > 1)) {
                acall = true;
            }
            if (config.fork_join > 0) {
                assert (t.async_ncpus > 0);
                if (exe != prev && exe.assign.async_idx != 0
                        && t.async_ncpus > 1) {
                    assert (acall == true);
                }
            }
            if (acall) {
                int async = 0;
                if (prev.call != null) {
                    if (prev.call.async > 0) { // async call
                        async = 1;
                        calls[async_called] = new Async_call();
                        calls[async_called].c = prev.c;
                        calls[async_called].call = prev.call;
                        calls[async_called].called = 1;
                        calls[async_called].parent_idx = prev.assign.async_idx;
                        calls[async_called].t = t;

                        assert (curr_e >= 0);
                        calls[async_called].release_t = do_RTA(t, curr_e, j[k], prev.cpu, prev, last_exe);
                        if (calls[async_called].release_t < 0) {
                            return -1;
                        }
                        assert (calls[async_called].release_t >= curr_e);
                        calls[async_called].v_release_t = 0;

                        calls[async_called].rt = async_RT(t, exe, calls[async_called].release_t, prev.c);

                        if (calls[async_called].rt < 0) {
                            return -1;
                        }

                        assert (calls[async_called].rt > calls[async_called].release_t);

                        while (!(exe.c.x == prev.c.x && exe.c.y == prev.c.y)) {
                            exe = exe.next;
                        }
                        assert (exe.c == prev.c);
                        assert (exe.cpu == prev.cpu);

                        if (exe.e_t == 0) {
                            assert (calls[async_called].call.async_return == 1);
                            assert (calls[async_called].call.async == 1);
                            assert (calls[async_called].call.fj_nseg >= 2);
                        }
                        if (calls[async_called].call.async_return == 1) {
                            if (calls[async_called].call.fj_nseg < 2) {
                                assert (t.async_subt[prev.next.assign.async_idx].async_merge < Long.MAX_VALUE);
                                assert (exe.e_t > 0);
                                long remaining = 0;
                                Exe go = exe;
                                while (go != null) {
                                    if (go.assign.async_idx == calls[async_called].parent_idx) {
                                        remaining += go.e_t;
                                    }
                                    go = go.next;
                                }
                                if (config.cost_inv == 0) {
                                    assert (remaining
                                            < t.async_subt[calls[async_called].parent_idx].wcet);
                                }
                                assert (calls[async_called].call.sync_point.t <= 1
                                        && calls[async_called].call.sync_point.t > 0);
                                calls[async_called].sync_remain =
                                        (long) (calls[async_called].call.sync_point.t
                                        * remaining);
                                assert (calls[async_called].sync_remain
                                        == t.async_subt[prev.next.assign.async_idx].async_merge - t.async_subt[prev.next.assign.async_idx].async_release);
                                if (calls[async_called].sync_remain == 0) {
                                    calls[async_called].sync_remain = 1;
                                }
                                assert (calls[async_called].sync_remain > 0);
                            } else {
                                // fork-join
                                calls[async_called].sync_remain = 0;
                            }
                            if (config.cost_inv == 0) {
                                assert (calls[async_called].sync_remain
                                        <= t.async_subt[exe.assign.async_idx].wcet);
                            }
                            async_called++;
                            if (exe.fj_seg == 1) { // FJ!!!!
                                prev = exe;
                                exe = exe.next;
                                continue;
                            }
                        } else {
                            calls[async_called] = null;
                        }

                    } // async call
                }
                if (async == 0) { // xcore inv

                    assert (false);
                    assert (curr_e > 0);
                    j[k + 1] = do_RTA(t, curr_e, j[k], prev.cpu, prev, last_exe);
                    checkout_sibling_if(t, prev);
                    if (j[k + 1] < 0) {
                        return -1;
                    }
                    k++;
                    curr_e = 0;
                    last_exe = prev;
                }
            }
            long tot_subprogress = 0;
            long last_progress = 0;

//            for (int i = 0; i < exe.c.n_async; i++) { // check sync points
//                int ii;
//                if (exe.c.sync_points[i].t < 0 || exe.assign.call_dep == false) {
//                    continue;
//                }
////                if ((exe.c.sync_points[i].t > c_progress[exe.c.x][exe.c.y])
//                        && (exe.c.sync_points[i].t <= c_progress[exe.c.x][exe.c.y] + exe.e_t)) {

            // sync point!!                
            while (true) {
                int sync_call = -1;
                if (config.fork_join == 0) {
                    for (int kk = 0; kk < async_called; kk++) { //which call is this sync for                        
                        assert (calls[kk].call.fj_nseg == 0);
                        if (calls[kk].synced > 0) {
                            continue;
                        }
                        if (exe.assign.async_idx != calls[kk].parent_idx) {
                            continue;
                        }
                        assert (calls[kk].sync_remain > 0);
                        if (calls[kk].sync_remain <= exe.e_t) {
                            if (sync_call == -1
                                    || calls[kk].sync_remain < calls[sync_call].sync_remain) {
                                sync_call = kk;
                            }
                        }
                        assert (calls[kk].synced == 0);
                        //break;
                    }
                } else {
                    for (int kk = 0; kk < async_called; kk++) { //which call is this sync for
                        if (calls[kk].synced > 0) {
                            continue;
                        }
                        if (calls[kk].call == exe.sync_call) {
                            sync_call = kk;
                            break;
                        }
                    }
                }

                if (sync_call < 0) {
                    break;
                }

                //found the call
                long sub_progress;
                assert (calls[sync_call].sync_remain >= last_progress);

                sub_progress = calls[sync_call].sync_remain - last_progress;
                last_progress = calls[sync_call].sync_remain;
                tot_subprogress += sub_progress;

                async_synced++;
                assert (calls[sync_call].synced == 0);
                calls[sync_call].synced = 1;

                assert (tot_subprogress <= exe.e_t);
                assert (curr_e + sub_progress >= 0);
                double max_rt, curr_rt;
                curr_rt = do_RTA(t, curr_e + sub_progress, j[k], exe.cpu, exe, last_exe);
                if (curr_rt < 0) {
                    return -1;
                }

                if (curr_rt >= calls[sync_call].rt) {
                    max_rt = curr_rt;
                    curr_e += sub_progress;
                } else {
                    checkout_sibling_if(t, exe);
                    max_rt = calls[sync_call].rt;
                    curr_e = 0;
                    last_exe = exe;
                    j[k] = max_rt;
                }

                // not using this approach anymore... just for sanity check now
//                for (int jj = 0; jj < async_called; jj++) {
//                    if (calls[jj].parent_idx != exe.assign.async_idx
//                            || calls[jj].synced > 0) {
//                        continue;
//                    }
//                    if (calls[jj].release_t < calls[sync_call].release_t) {
//                        continue;
//                    }
//                    assert (calls[jj].synced == 0);
//                    assert (calls[jj].sync_remain >= calls[sync_call].sync_remain);
//
//                    double overlap;
//
//                    assert (max_rt >= calls[jj].v_release_t);
//                    if (calls[jj].v_release_t == 0) {
//                        overlap = max_rt - calls[jj].release_t;
//                    } else {
//                        overlap = max_rt - calls[jj].v_release_t;
//                    }
//                    calls[jj].v_release_t = max_rt;
////                    assert (overlap >= 0);
////                    if (calls[jj].rt > overlap) {
////                        calls[jj].rt -= overlap;
////                    } else {
////                        calls[jj].rt = 0;
////                    }
//                }
            }
            //} // sync points done

            assert (exe.e_t >= tot_subprogress);
            curr_e += exe.e_t - tot_subprogress;

            assert (curr_e >= 0);
            if (config.cost_inv == 0) {
                assert (curr_e <= t.wcet);
            }
            c_progress[exe.c.x][exe.c.y] += exe.e_t;
            if (exe.call == null) {
                if (config.cost_inv == 0) {
                    assert (c_progress[exe.c.x][exe.c.y] == exe.c.e);
                } else {
                    assert (c_progress[exe.c.x][exe.c.y] >= exe.c.e);
                }
                c_progress[exe.c.x][exe.c.y] = 0;
            }
            if (config.cost_inv == 0) {
                assert (c_progress[exe.c.x][exe.c.y] <= exe.c.e);
            }

            if (config.fork_join == 0) {
                for (int i = 0; i < async_called; i++) {// update sync remaining
                    if (calls[i].synced > 0) {
                        continue;
                    }

                    assert (calls[i].synced == 0);
                    assert (calls[i].call.async_return == 1);
                    assert (calls[i].call.async == 1);
                    if (calls[i].parent_idx == exe.assign.async_idx) {

                        assert (calls[i].sync_remain > exe.e_t);
                        calls[i].sync_remain -= exe.e_t;
                    }
                    assert (calls[i].sync_remain >= 0);
                }
            }

            prev = exe;
            exe = exe.next;

            int finish = 0;
            if (exe == null) {
                finish = 1;
            }
            if (end != null) {
                if (end.x == exe.c.x && end.y == exe.c.y) {
                    finish = 1;
                }
            }
            if (finish > 0) {
                double rt = do_RTA(t, curr_e, j[k], prev.cpu, prev, last_exe);
                checkout_sibling_if(t, prev);
                if (rt < 0) {
                    return -1;
                } else {
                    assert (rt <= t.d);
                    return rt;
                }
            }
        } // while 1 loop        
    }

    int holistic_async(Task new_t) {
        int i;
        //i = new_t.id;
        i = 0;
        int highest = -1;
        for (; i < config.ntasks; i++) {
            assert (task[i].wcet == Math.round((task[i].p * task[i].u)));
            if (task[i].assigned == 0) {
                continue;
            }
            if (highest < 0) {
                highest = i;
            }
            Task curr_t = task[i];

            curr_t.async_subt[0].exe_head = curr_t.exe_head.next;
            curr_t.async_subt[0].sibling_if_taken = 1;
            for (int j = 1; j < curr_t.n_async + 1; j++) {
                Task sub_t = curr_t.async_subt[j];
                sub_t.sibling_if_taken = 0;
                Exe exe = curr_t.exe_head.next, prev = null;
                long sum = 0;
                while (exe.assign != sub_t.assign_head.next) {
                    assert (exe != null);
                    if (exe.assign.async_idx == sub_t.assign_head.next.parent_async_idx) {
                        sum += exe.e_t;
                    }
                    prev = exe;
                    exe = exe.next;
                }
                sub_t.async_release = sum;
                sub_t.async_call = prev.call;
                sub_t.exe_head = exe;
                sum = 0;
                if (config.FJ_nomain > 0) {
//                    while (exe.sync_call != sub_t.async_call) {
//                        exe = exe.next;
//                    }
                    if (config.cost_inv == 0) {
                        assert (sub_t.async_release == 0);
                    }
                } else {
                    while (exe != null) {
                        if (exe.assign.async_idx == sub_t.assign_head.next.parent_async_idx) {
                            sum += exe.e_t;
                        }
                        exe = exe.next;
                    }
                    //sum is remaining
                    if (sub_t.async_call.async_return == 0) {
                        assert (sub_t.async_call.sync_point.t == -1);
                        sub_t.async_merge = Long.MAX_VALUE;
                    } else {
                        assert (sub_t.async_call.sync_point.t > 0
                                && sub_t.async_call.sync_point.t <= 1);
                        sub_t.async_merge = sub_t.async_release
                                + (long) (sum * sub_t.async_call.sync_point.t);
                    }
                }
            }
            //int l = task[i].path.length;
            //double r[] = new double[l], r1;
            //double jitter[] = new double[l];            

            Exe exe = curr_t.exe_head.next;
            double rt = async_RT(curr_t, exe, 0, null);
//            if (i == highest) {
//                if (rt < 0) {
//                    i--;
//                    continue;
//                }
//            }
            if (rt < 0) {
                return -1;
            }
            assert (rt <= curr_t.d);
        }
        return 0;
    }

    int holistic_d_lessthan_p(Task new_t) {
        int i, j, k;
        // only need to redo for current and lower threads!
        i = new_t.id;
        for (; i < config.ntasks; i++) {
            assert (task[i].wcet == Math.round((task[i].p * task[i].u)));
            if (task[i].assigned == 0) {
                continue;
            }

            int l = task[i].path.length;

            boolean converged[] = new boolean[l];

            double r[] = new double[l], r1;
            double jitter[] = new double[l];
            for (k = 0; k < l; k++) {
                r[k] = 0;
                jitter[k] = 0;
            }
            while (true) {
                // iterate each stage:
                Core_node cn;
                cn = task[i].path.stage_list.next;
                assert (cn != null);
                int cid;
                for (k = 0; k < l; k++, cn = cn.next) {
                    cid = cn.c.id;
                    r1 = cn.e;
                    assert (core[cid].assigned_head[i].next != null);

                    for (j = i - 1; j >= 0; j--) {
                        if (core[cid].assigned_head[j].next == null) {
                            continue;
                        }
                        if (converged[k] == true) {
                            r1 = r[k];
                            break;
                        }
                        //double e = core[cid].sum_assign[j];
                        Core_node nn = task[j].path.stage_list.next;
                        assert (nn != null);
                        while (nn != null) {
                            if (task[j].path.length == 1) {
                                assert (nn.jitter == 0);
                            }
                            if (nn.c.id == cid) {
                                r1 += (long) (Math.ceil((r[k] + nn.jitter) / task[j].p) * nn.e);
                            }
                            nn = nn.next;
                        }
                    }

                    if (r1 == r[k]) {
                        converged[k] = true;
                    }
                    if (r1 + jitter[k] > task[i].d) {
                        return -1;
                    }
                    r[k] = r1;
                    if (k + 1 < l) {
                        jitter[k + 1] = r1 + jitter[k];
                    }
                }
                assert (cn == null);
                boolean finish = true;
                for (k = 0; k < l; k++) {
                    if (converged[k] == false) {
                        finish = false;
                        break;//              System.out.println(i + " " + yes);
                    }
                }
                if (finish == true) {
                    cn = task[i].path.stage_list.next;
                    for (k = 0; k < l; k++, cn = cn.next) {
                        cn.jitter = jitter[k];
                    }
                    break; // this thread is scheduable.
                }
            }
        }
        return 0;
    }

    int RTA(Task new_t) {
        int i, j;
        // only need to redo for current and lower threads!
        i = new_t.id;
        for (; i < config.ntasks; i++) {
            assert (task[i].wcet == Math.round((task[i].p * task[i].u)));
            if (task[i].assigned == 0) {
                continue;
            }
            long r0 = 0, r1 = task[i].wcet;
            boolean yes = false;
            while (true) {
                int cc;

                for (cc = 0; cc < config.ncpus; cc++) {
                    if (core[cc].assigned_head[i].next == null) {
                        continue;
                    }

                    for (j = i - 1; j >= 0; j--) {
                        if (core[cc].assigned_head[j].next == null) {
                            continue;
                        }
                        for (int ii = 0; ii < config.ncpus; ii++) {
                            if (ii != cc) {
                                assert (core[ii].assigned_head[j].next == null);
                            }
                        } // sanity check only.
                        long e = 0;
//                        Assign a = core[cc].assigned_head[j].next;
//                        while (a != null) {
//                            e += a.c.e;
//                            a = a.next;
//                        }
                        Exe exe = task[j].exe_head.next;
                        while (exe != null) {
                            e += exe.e_t;
                            exe = exe.next;
                        }
                        if (j != i || r0 == 0) {
                            r1 += (long) (Math.ceil(r0 / task[j].p) * e);
                        } else {
                            //For d > p
                            assert ((long) (Math.ceil(r0 / task[j].p) * e) >= 0);
                            r1 += (long) (Math.ceil(r0 / task[j].p) * e); // no difference
                        }
                    }
                }
//                for (j = i - 1; j >= 0; j--) {
//                    r1 += (long) (Math.ceil(r0 / task[j].p) * task[j].wcet);
//                }
                if (r1 == r0) {
                    yes = true;
                    break;
                }
                if (r1 > task[i].d) {
                    break;
                }
                r0 = r1;
                r1 = task[i].wcet;
            }
            if (yes == false) {
                //              System.out.println(i + " " + yes);
                return -1;
            }
        }
        return 0;
    }

    void calc_nodemax(Task t) {
        int i, j, k;
        t.nodemax = new long[config.ncpus];
        for (i = 0; i < config.ncpus; i++) {
            t.nodemax[i] = 0;
        }
        // QW: higher or include current? Include!
        for (i = t.id; i >= 0; i--) {
            if (task[i].assigned == 0) {
                continue;
            }
            Fold f = task[i].fold_list.next;

            for (j = 0; j < task[i].folds; j++) {
                for (k = 0; k < f.length; k++) {
                    if (t.alloc_core[f.stage[k].id] == 1
                            && t.nodemax[f.stage[k].id] < f.e[k]) {
                        t.nodemax[f.stage[k].id] = f.e[k];
                    }
                }
                f = f.next;
            }
        }
        //for (i = 0; i < config.ncpus; i++) {
        //System.out.println(nodemax[i]);
        //}

    }

    int praveen(int optimized, Task new_t) {

        long[] cmax = new long[config.ntasks];

        //int curr_tid = new_t.id;
        int i, j, k;
        //calc cmax for each thd
        for (i = 0; i < config.ntasks; i++) {
            if (task[i].assigned == 0) {
                continue;
            }
            Fold f = task[i].fold_list.next;
            for (j = 0; j < task[i].folds; j++, f = f.next) {
                for (k = 0; k < f.length; k++) {
                    if (f.e[k] > cmax[i]) {
                        cmax[i] = f.e[k];
                    }
                }
            }
        }

        for (i = 0; i < config.ntasks; i++) {
            if (task[i].assigned == 0) {
                continue;
            }
            assert (task[i].wcet == Math.round((task[i].p * task[i].u)));
            long r0 = 0, r1;
            Seg s = task[i].high_segs.next;

            if (task[i].folds == 1 && task[i].fold_list.next.length == 1) {
                // only assigned to a single core
                task[i].v_e = task[i].wcet;
                r1 = task[i].v_e;
                while (s != null) {
                    assert (s.length == 1);
                    assert (s.cmax == s.e[0]);
                    assert (s.type == 1);
                    assert (s.cmax > 0);
                    s.v_e = s.cmax;
                    s = s.next;
                }
            } else {
                int forward_touched[] = new int[config.ncpus];
                s = task[i].high_segs.next;
                while (s != null) {
                    assert (s.cmax > 0);
                    if (optimized > 0) {
                        assert (s.cmax * 2 > s.cmax);
                        if (s.type == 0) {
//                            assert (false);
                            s.v_e = s.cmax * 2;
                            for (int ss = 0; ss < s.length; ss++) {
                                forward_touched[s.stage[ss].id] = 1;
                            }
                        } else {
                            //System.out.println("opt");
                            s.v_e = s.cmax;
                        }
                    } else {
                        for (int ss = 0; ss < s.length; ss++) {
                            forward_touched[s.stage[ss].id] = 1;
                        }
                        s.v_e = s.cmax * 2;
                    }

                    s = s.next;
                }

                if (optimized > 0) {
                    int jj, kk;
                    long thiscmax = 0;
                    Fold f = task[i].fold_list.next;
                    for (jj = 0; jj < task[i].folds; jj++, f = f.next) {
                        for (kk = 0; kk < f.length; kk++) {
                            if (f.e[kk] > thiscmax && forward_touched[f.stage[kk].id] == 1) {
                                thiscmax = f.e[kk];
                            }
                        }
                    }
                    r1 = thiscmax;
                    //r1 = cmax[i];
                    assert (r1 <= cmax[i]);
                } else {
                    r1 = cmax[i];
                }
                calc_nodemax(task[i]);
                int ii;
                Core_node cn = task[i].path.stage_list.next;
                for (ii = 0; ii < task[i].path.length; ii++, cn = cn.next) {
                    if (forward_touched[cn.c.id] == 0 && optimized > 0) {
                        r1 += cn.e;
                        //r1 += task[i].nodemax[cn.c.id];
                    } else {
                        r1 += task[i].nodemax[cn.c.id];
                    }
                    assert (task[i].nodemax[cn.c.id] >= cn.e);
                }
                assert (cn == null);
                task[i].v_e = r1;
                assert (task[i].v_e >= task[i].wcet);
            }

            boolean yes = false;
            while (true) {
                s = task[i].high_segs.next;

                while (s != null) {
                    assert (s.t != null);
                    int d_p = (int) Math.ceil(s.t.d / s.t.p);
                    if (s.t.u <= 1) {
                        assert (d_p == 1);
                    }
                    r1 += s.v_e * Math.ceil(r0 / s.t.p) * d_p;
                    assert (s.v_e > 0);
                    s = s.next;
                }

                long ipi_sum = 0;
                //interference from IPIs!
                for (int coreid = 0; coreid < config.ncpus; coreid++) {
                    if (task[i].alloc_core[coreid] == 1) {
                        int jj;
                        for (jj = 0; jj < config.ntasks; jj++) {
                            if (task[jj].assigned == 0) {
                                continue;
                            }
                            long e = this.thd_core[jj][coreid] * config.cost_ipi_top_half;
                            ipi_sum += e * Math.ceil(r0 / task[jj].p);
                        }
                    }
                }
                //done IPI interference.
                r1 += ipi_sum;

                if (r1 == r0) {
                    yes = true;
                    break;
                }
                if (r1 > task[i].d) {
                    yes = false;
                    break;
                }
                r0 = r1;
                r1 = task[i].v_e;
            }
            if (yes == false) {
                //              System.out.println(i + " " + yes);
                return -1;
            }
        }
        return 0;
    }

    int calc_folds(Task new_t) {
        int i;
        Core cpu;

//        for (i = 0; i < config.ntasks; i++) {
        //if (task[i].assigned == 0) {
//                continue;
//            }
        i = new_t.id;
//        assert (task[i].assigned > 0);

        task[i].fold_init();
        Fold f = null;

        Exe e = task[i].exe_head.next, old_e;
        old_e = e;
        double sum = 0;

        while (e != null) {
            cpu = e.cpu;
            assert (e.cpu != null);
            Fold new_f = new Fold();

            if (f == null) {
                task[i].fold_list.next = new_f;
            } else {
                f.next = new_f;
            }
            f = new_f;

            task[i].folds++;
            f.add(task[i], cpu);

            while (e != null) {
                if (old_e.cpu.id != e.cpu.id) {
                    if (f.cpu[e.cpu.id] == 1) {
                        old_e = e;
                        break; // need a new fold
                    } else {
                        f.add(task[i], e.cpu);
                    }
                }
                old_e = e;
                assert (e.e_t >= 0);
                f.e[f.length - 1] += e.e_t;
                sum += e.e_t;
                e = e.next;
            }
        }

        assert (sum >= new_t.wcet);
        //      }
        // just need to re-calc lower prio thds!
        i = new_t.id;
        for (; i < config.ntasks; i++) {
            if (task[i].assigned == 0) {
                continue;
            }
            task[i].calc_high_segs();
        }
        return 0;
    }

    int generate(Config info) {
        config = info;
        w = config.width;
        d = config.depth;
        int ret;
        int iter = 0;
        do {
            iter++;
            if (iter > 10000) {
                System.out.println("Hard to generate...");
                iter = 0;
            }
            this.generate_graph();
            ret = this.generate_invs();
            if (ret < 0) {
                continue;
            }
            ret = this.generate_tasks();
            //System.out.print("x");
        } while (ret < 0);
        for (int i = 0; i < config.ntasks; i++) {
            calc_assign_chain(task[i]);
        }

        return 0;
    }

    int init() {

        int i, j, ntasks = config.ntasks;

        init_core();

        for (i = 0; i < ntasks; i++) {
            task[i].assigned = 0;
        }
        this.thd_core = new int[config.ntasks][config.ncpus];

        return 0;
    }

    class Glb_sim {

        class T {

            long progress = 0;
            long p;
            double d;
            long e;
            int prio;
            //Evt evt;

            public T(long e, long p, long d, int prio) {
                this.p = p;
                this.d = d;
                this.e = e;
                this.prio = prio;

            }

            public T() {
            }
        }

        class Job extends T {

            //int cpu; // -1 means inactive
            double start = -1;
            T t;

            public Job(T orig_t, double release) {
                this.t = orig_t;
                this.p = orig_t.p;
                this.d = orig_t.d + release;
                this.e = orig_t.e;
                this.prio = orig_t.prio;
            }
        }

        class Qnode {

            Job j;
            Qnode next, prev;
        }

        class RQ {

            Qnode head; // prio, high -> low
            Qnode tail;
            int length;

            int init() {
                head = new Qnode();
                tail = new Qnode();
                head.next = tail;
                head.prev = null;
                tail.next = null;
                tail.prev = head;
                length = 0;
                return 0;
            }

            int add(Job newj) {
                Qnode newnode = new Qnode();
                newnode.j = newj;
                Qnode currnode;
                currnode = head;

                while (currnode.next != tail) {
                    if (currnode != head) {
                        assert (currnode.j.p <= currnode.next.j.p);
                    }
                    if (newnode.j.prio < currnode.next.j.prio) {
                        break;
                    }
                    currnode = currnode.next;
                    assert (currnode != null);
                }

                newnode.prev = currnode;
                newnode.next = currnode.next;

                currnode.next.prev = newnode;
                currnode.next = newnode;

                this.length++;

                return 0;
            }

            int remove_job(Job j) {
                Qnode n = head;
                while (n.next.j != j) {
                    if (n != head) {
                        assert (n.j.prio <= n.next.j.prio);
                    }
                    n = n.next;
                }
                assert (n != null);
                n.next.next.prev = n;
                n.next = n.next.next;
                this.length--;
                return 0;
            }

            Job remove_high() {
                Qnode ret = head.next;
                if (ret == tail) {
                    return null;
                }
                head.next = ret.next;
                ret.next.prev = head;
                this.length--;
                if (head.next != tail) {
                    assert (ret.j.p <= head.next.j.p);
                }
                return ret.j;
            }

            Job remove_low() {
                Qnode ret = tail.prev;
                if (ret == head) {
                    return null;
                }
                tail.prev = ret.prev;
                ret.prev.next = tail;
                this.length--;
                if (tail.prev != head) {
                    assert (ret.j.p >= tail.prev.j.p);
                }
                return ret.j;
            }

            Qnode get_low() {
                assert (tail.prev != head);
                return tail.prev;
            }
        }

        class CPU {

            T t;
        }

        class Evt {

            double time;
            int type; // 1 -> finish execution, 2 -> new release (period)
            Job j;
            T t;
            Evt next;

            public Evt(Job j, double time, int type) {
                this.j = j;
                this.time = time;
                this.type = type;
                assert (type == 1);
            }

            public Evt(T t, double time, int type) {
                this.t = t;
                this.time = time;
                assert (time % t.p == 0);
                this.type = type;
                assert (type == 2);
            }

            public Evt() {
            }
        }

        class Evtq {

            Evt head = new Evt();

            int add(Job j, double time, int type) {
                Evt newevt;
                if (type == 1) {
                    newevt = new Evt(j, time, type);
                } else {
                    newevt = new Evt(j.t, time, type);
                }
                Evt curr = head;
                while (curr.next != null) {
                    assert (curr.time <= curr.next.time);
                    if (curr.next.time >= newevt.time) {
                        if (curr.next.time == newevt.time) {
                            if (newevt.type == 2) {
                                while (curr.next.time == newevt.time) {
                                    curr = curr.next;
                                    if (curr.next == null) {
                                        break;
                                    }
                                }
                                if (curr.next != null) {
                                    assert (curr.next.time > curr.time);
                                }
                            }
                        } // always keep completion first.

                        break;
                    }
                    curr = curr.next;
                }
                if (curr != head) {
                    assert (curr.time <= newevt.time);
                }
                if (curr.next != null) {
                    assert (newevt.time <= curr.next.time);
                }
                newevt.next = curr.next;
                curr.next = newevt;

                return 0;
            }

            int rem_evt(Job j) {
                // we can only remove completion events
                int removed = 0;
                Evt curr = head;
                while (true) {
                    if (curr.next == null) {
                        break;
                    }
                    if (curr.next.type == 1) {
                        if (curr.next.j == j) {
                            //found it
                            curr.next = curr.next.next;
                            removed++;
                            break;
                        }
                    }
                    if (curr.next != null) {
                        assert (curr.time <= curr.next.time);
                    }
                    curr = curr.next;
                }
                assert (removed == 1);

                return 0;
            }

            Evt take_evt() {
                Evt ret = head.next;
                if (ret != null) {
                    head.next = ret.next;
                }
                if (head.next != null) {
                    assert (ret.time <= head.next.time);
                }
                return ret;
            }
        }
        // done with classes!!
        RQ runq;
        RQ waitq;
        Evtq evtq;

        int start_job(Job j, double curr_time) {
            assert (runq.length < config.ncpus);
            if (waitq.head.next != waitq.tail) {
                assert (j.p <= waitq.head.next.j.p);
            }
            runq.add(j);
            assert (j.e > j.progress);
            j.start = curr_time;
            evtq.add(j, curr_time + j.e - j.progress, 1); // finish execution
            assert (runq.length <= config.ncpus);

            return 0;
        }

        int stop_job(Job j, double curr_time) {
            evtq.rem_evt(j);
            assert (j.start >= 0);
            double progress = curr_time - j.start;
            j.progress += progress;
            assert (j.progress < j.e);
            if (waitq.head.next != waitq.tail) {
                assert (j.p <= waitq.head.next.j.p);
            }
            waitq.add(j);

            return 0;
        }

        double gcd(double m, double n) {
            if (n == 0) {
                return m;
            } else {
                return gcd(n, m % n);
            }
        }

        double upper_limit() {
//            return Math.pow(10, config.maxpower);
            return task[config.ntasks - 1].p * 1024;
        }

        double calc_lcm(double t[], int length) {
            double second, first, ret, max, min;
            first = t[length - 1];

            if (length > 2) {
                second = calc_lcm(t, length - 1);
            } else {
                second = t[0];
            }

            if (first > second) {
                max = first;
                min = second;
            } else {
                max = second;
                min = first;
            }
            //doing lcm between first and second
            if (max > upper_limit()) {
                return upper_limit();
            }

            double ggg = gcd(max, min);

            assert (min % ggg == 0);
            assert (max % ggg == 0);

            ret = (min / ggg) * max;

            return ret;
        }

        double get_lcm(T t[], int length) {
            double p[] = new double[length];
            double ret;
            int i;
            long div = 1;
            for (i = 0; i < length; i++) {
                p[i] = Math.ceil(t[i].p / div);
            }
//
//            p[0] = 48; 
//            p[1] = 8;
//            p[2] = 16;
//            p[3] = 32;            
//            ret = calc_lcm(p, 4);
            ret = calc_lcm(p, length);

            ret *= div;

            if (ret > upper_limit()) {
                ret = upper_limit();
            }

            return ret;
        }

        int start() {
            int i, j, k;
            double total_p = 0;
            init();
            T allt[] = new T[config.ntasks];
            runq = new RQ();
            waitq = new RQ();
            evtq = new Evtq();

            runq.init();
            waitq.init();
            for (i = 0; i < config.ntasks; i++) {
                allt[i] = new T(task[i].wcet, Math.round(task[i].p), Math.round(task[i].d), i);
                if (i - 1 >= 0) {
                    assert (allt[i].p >= allt[i - 1].p);
                }
            }

//            for (i = 0; i < config.ntasks; i++) {
//                total_p += task[i].p;
//            }
//            System.out.println("avg p " + total_p / config.ntasks);

            for (i = 0; i < config.ntasks; i++) {
                Job job = new Job(allt[i], 0);
                if (i < config.ncpus) {
                    start_job(job, 0);// finish execution
                } else {
                    waitq.add(job);
                }

                evtq.add(job, 0 + job.p, 2); // next release
            }

            double lcm = get_lcm(allt, config.ntasks);
            // ready to go!!!
            double time, last_time = 0, diff;
            while (true) {
                Evt e = evtq.take_evt();

                if (e == null) {
                    assert (last_time > lcm);
                    break; // schedulable!!
                }
                time = e.time;
                diff = time - last_time;
                assert (diff >= 0);
                if (e.type == 1) { // someone completed!
                    assert (e.j.e - e.j.progress == time - e.j.start);
                    assert (e.j.t.d == e.j.t.p);
                    if (time > e.j.d) {
                        return -1; // late!!!
                    }
                    runq.remove_job(e.j);
                    assert (e.j.d >= time);

                    if (waitq.length > 0) {
                        assert (runq.length < config.ncpus);
                        Job waitingjob = waitq.remove_high();
                        if (waitq.length > 0) {
                            assert (waitingjob.p <= waitq.head.next.j.p);
                        }
                        assert (waitingjob.progress < waitingjob.e);

                        start_job(waitingjob, time);
                        assert (runq.tail.prev.j == waitingjob);
                    }
                } else { // we got a new release!
                    assert (e.type == 2);
                    assert (runq.length <= config.ncpus);
                    Job newjob = new Job(e.t, time);

                    if (runq.length < config.ncpus) {
                        //just run it
                        start_job(newjob, time);
                    } else {
                        // runqueue full. Let's see.
                        if (e.t.prio < runq.get_low().j.prio) {
                            // we should get running

                            //take one off
                            Job low = runq.remove_low();
                            assert (low.prio > e.t.prio);
                            assert (low.p > e.t.p);
                            stop_job(low, time);

                            //put the new one on
                            start_job(newjob, time);
                        } else {
                            waitq.add(newjob); // not able to run
                        }
                    }

                    if (time <= lcm) {
                        //create new release
                        evtq.add(newjob, time + newjob.p, 2); // next release
                    }
                }
                last_time = time;
            }


            System.out.print("");

            return 0;
        }
    }

    int global_simulation() {
        int ret;
        Glb_sim glb = new Glb_sim();
        ret = glb.start();

        return ret;
    }

    int process() {
        int ret;

        init();// init done.

        if (config.method < 4) {
            ret = this.assign();
        } else if (config.method == 4) {
            ret = this.assign_comp();
        } else {
            ret = this.assign_async();
        }


        //ret = analysis(task[0]);// for debug, for fun....
//        if (method == 0) {
//            ret = this.RTA();
//            if (ret < 0) {
//                //System.out.println("RTA fail...");
//            }
//        } else if (method == 1) {
//            ret = this.praveen(0);
//        } else {
//            ret = this.praveen(1);
//        }

        return ret;
    }

    public Graph() {
    }
}

class Worker implements Runnable {

    Thread t;
    //Config config;
    double u, umax, umin;
    Config var_cfg;

    Worker(double u, double umax, double umin, Config cfg) {
        // Create a new, second thread
        t = new Thread(this, "worker");
        this.u = u;
        this.umax = umax;
        this.umin = umin;
        this.var_cfg = cfg;
        t.start(); // Start the thread
    }

    int do_both(Graph g, Config config) {
        int ret;
//        config.LPF = 1;
//        ret = g.process();
//        return ret;
        config.task_order = 0;
        ret = g.process();
        if (ret < 0) {
            config.task_order = 1;
            ret = g.process();
        }
        return ret;
    }
    // This is the entry point for the second thread.

    @Override
    public void run() {
        Config config = new Config();
        double tot = config.avg_over;

//        for (double u = 1; u < config.ncpus; u++) {
//        double u = 28;
//        {        
        //config.invs_mean = this.var_cfg.invs_mean;
        //config.async_return = this.var_cfg.async_return;
        //config.async_release_offset = this.var_cfg.async_release_offset;
        //config.fork_join_max_seg = this.var_cfg.fork_join_max_seg;        
        config.e_mean = this.var_cfg.e_mean;
        config.e_var = config.e_mean;
        //

        config.u_mean = (this.umax + this.umin) / 2;
        config.u_max = this.umax;
        config.u_min = this.umin;
        config.set_U(u);
        int ret1 = 0, ret2 = 0, ret3 = 0, ret4 = 0,
                ret5 = 0, ret6 = 0, ret7 = 0, ret8 = 0, ret9 = 0;

        double fail1 = 0, fail2 = 0, fail3 = 0, fail4 = 0,
                fail5 = 0, fail6 = 0, fail7 = 0, fail8 = 0, fail9 = 0;
        double path_d_ratio = 0;

        for (int i = 0; i < tot; i++) {
            Graph g;
            //System.out.println(i);
            if (config.do_partition > 0) {
                g = new Graph();
                g.generate(config);

                config.split = 0;
                config.method = 0;//partition
                ret1 = do_both(g, config);
                if (ret1 < 0) {
                    fail1++;
                }

                config.split = 1;
                config.method = 1;
                ret2 = do_both(g, config);//orig Praveen's
                if (ret2 < 0) {
                    fail2++;
                }
                assert (ret2 >= ret1);

                config.method = 2;
                ret3 = do_both(g, config);//optimized Praveen's
                if (ret3 < 0) {
                    fail3++;
                }
                assert (ret3 >= ret1);
                if (ret2 != ret3) {
//                System.out.println("different!!!!");
                }
                config.method = 3;
                ret4 = do_both(g, config);//holistic
                if (ret4 < 0) {
                    fail4++;
                }
                //assert (ret4 >= ret);
                //if (ret1 < 0 && ret2 < 0 && ret3 < 0 && ret4 < 0) {

                //}
                if (config.do_comp2core < 0) {
                    config.split = 1;
                    config.method = 4; // comp -> core
                    //ret5 = g.process();
                    if (ret5 < 0) {
                        fail5++;
                    }
                }
            }
            if (config.do_global > 0) {
                g = new Graph();
                g.generate(config);

                config.method = 5; // global simulation

                ret6 = g.global_simulation();
                if (ret6 < 0) {
                    fail6++;
                }
            }

            if (config.do_async > 0) {
                g = new Graph();
                config.method = 6; // with async
                config.split = 1;

                g.generate(config);

                for (int j = 0; j < config.ntasks; j++) {
                    path_d_ratio += g.task[j].last_rt / g.task[j].d;
                }
                //System.out.println("Trying async...");
                ret7 = g.process();

                //ret7 = do_both(g, config);
                if (ret7 < 0) {
                    fail7++;
                } else {
                    //System.out.println("Async pass!!!!!!!!!!!!!!!!");
                }
            }

            if (config.do_fj > 0) {
                // Fork join simulation. generate new graph for it
//                g = new Graph();
//                config.method = 6; // with async
//                config.split = 0;
//                config.task_order = 2;
//
//                config.fork_join = 1;
//                g.generate(config);
//
//                ret8 = g.process();
//                if (ret8 < 0) {
//                    fail8++;
//                } else {
//                    //System.out.println("Async pass!!!!!!!!!!!!!!!!");
//                }
                //
                //next, the 20% FJ
                Config normal = config;
                config = new Config();
                config.e_mean = normal.e_mean;
                config.e_var = config.e_mean;
                config.u_mean = (this.umax + this.umin) / 2;
                config.u_max = this.umax;
                config.u_min = this.umin;
                config.set_U(u);

                config.depth = 2;
                config.inv_jump_mean = 1;
                config.inv_jump_var = 0;
                config.home_level_mean = 1;
                config.home_level_var = 0;

                config.method = 6; // with async
                config.split = 0;
                //config.task_order = ;

                config.fork_join = 1;
                config.FJ_nomain = 1;
                config.fork_join_max_seg = 20;
                //config.fork_join_max_seg = this.var_cfg.fork_join_max_seg;

                config.invs_mean = 10;
                config.invs_max = (int) config.invs_mean + 1;
                config.invs_var = 0;

                config.width_mean = 2 * (int) config.invs_mean;
                config.width = 2 * config.width_mean;
                config.async_r = 1;
                config.async_return = 1;
                //config.async_only_partiton = 1;
                g = new Graph();
                g.generate(config);
                for (int j = 0; j < config.ntasks; j++) {
                    path_d_ratio += g.task[j].last_rt / g.task[j].d;
                }

                ret9 = g.process();
                if (ret9 < 0) {
                    fail9++;
                } else {
                    //System.out.println("Async pass!!!!!!!!!!!!!!!!");
                }
                //ret9 = g.process();

                config = normal;
                //
                config.fork_join = 0;
            }
        }
        if (config.do_partition > 0) {
            System.out.println("pt_u " + config.u_mean + " NO split RTA, " + u + " " + ((double) 1 - (fail1 / tot)));
            //System.out.println("split & RTA, " + u + " " + "success rate, " + ((double) 1 - (fail1 / tot)));
            System.out.println("pt_u " + config.u_mean + " split, orig P, " + u + " " + ((double) 1 - (fail2 / tot)));
            System.out.println("pt_u " + config.u_mean + " split, optimized P, " + u + " " + ((double) 1 - (fail3 / tot)));
            System.out.println("pt_u " + config.u_mean + " split, holistic analysis, " + u + " " + ((double) 1 - (fail4 / tot)));
            //System.out.println("pt_u " + config.u_mean + " Comp assignment,RTA analysis, " + u +  " " + ((double) 1 - (fail5 / tot)));
        }
        if (config.do_async > 0) {
            //System.out.println(config.invs_mean + " pt_u " + config.u_mean + " split w/ async, holistic analysis, " + u + " " + ((double) 1 - (fail7 / tot)) + " ratio: " + path_d_ratio / config.ntasks / tot);
            //System.out.println(config.async_return + " pt_u " + config.u_mean + " split w/ async, holistic analysis, " + u + " " + ((double) 1 - (fail7 / tot)) + " ratio: " + path_d_ratio / config.ntasks / tot);
            //System.out.println(config.async_release_offset + " pt_u " + config.u_mean + " split w/ async, holistic analysis, " + u + " " + ((double) 1 - (fail7 / tot)) + " ratio: " + path_d_ratio / config.ntasks / tot);
            System.out.println(config.e_mean / 2400 + " pt_u " + config.u_mean + " split w/ async, holistic analysis, " + u + " " + ((double) 1 - (fail7 / tot)) + " ratio: " + path_d_ratio / config.ntasks / tot);
            //System.out.println("pt_u " + config.u_mean + " split w/ async, holistic analysis, " + u + " " + ((double) 1 - (fail7 / tot)) + " ratio: " + path_d_ratio / config.ntasks / tot);
        }
        if (config.do_fj > 0) {
            //System.out.println("pt_u " + config.u_mean + " async w/ fork join, holistic analysis, " + u + " " + ((double) 1 - (fail8 / tot)));
            //System.out.println("pt_u " + config.u_mean + " async w/ FJ,no_main_thd,holistic, " + u + " " + ((double) 1 - (fail9 / tot)) + " ratio: " + path_d_ratio / config.ntasks / tot);
            //System.out.println(config.fork_join_max_seg / 2 + " pt_u " + config.u_mean + " async w/ FJ,no_main_thd,holistic, " + u + " " + ((double) 1 - (fail9 / tot)) + " ratio: " + path_d_ratio / config.ntasks / tot);
            System.out.println(config.e_mean / 2400 + " pt_u " + config.u_mean + " async w/ FJ,no_main_thd,holistic, " + u + " " + ((double) 1 - (fail9 / tot)) + " ratio: " + path_d_ratio / config.ntasks / tot);
        }

        if (config.do_global > 0) {
            System.out.println("pt_u " + config.u_mean + " global__________________________,  , " + u + " " + ((double) 1 - (fail6 / tot)));
        }

    }
}

public class Mc {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        System.out.println("Start...");
        Config cfg = new Config();
        double u;
        //double[] ptu = new double[]{1, 2, 3, 4, 6, 8};
        //double[] var = new double[]{1.0, 0.9, 0.8, 0.7, 0.6, 0.5, 0.4, 0.3, 0.2, 0.1, 0};
        //double[] var = new double[]{0.9, 0.8, 0.7, 0.6, 0.5, 0.4, 0.3, 0.2, 0.1};
        //double[] var = new double[]{1.5, 2, 2.5, 3, 3.5, 4, 4.5};
        //double[] var = new double[]{4, 6, 8, 10, 12, 14, 16};
        //long[] var = new long[]{25, 50, 100, 200, 400, 800, 1600};
        //long[] var = new long[]{100000};

        long strand_e = 15;

        //double[] ptu = new double[]{0.7};
        //double[] ptu = new double[]{1.5};
        double[] ptu = new double[]{3};
        int k = 0;
        System.out.println("avg over " + cfg.avg_over);
        //while (k < var.length) {
        for (u = ptu[0]; u <= 30; u += ptu[0]) {
//            for (u = 24; u <= 24; u += 1) {

            //u = 12.8;
            //u = 16;
            //u = 20.8;
            //u = 24;
            double max, min;
            max = ptu[0] + 1;
            min = ptu[0] - 1;
            //max = ptu[0] + 0.5;
            //min = ptu[0] - 0.5;

            //max = 1;
            //min = 0.4;

            //max = ptu[0] * (1 + 0.5);
            //min = ptu[0] * (1 - 0.5);

            cfg = new Config();
            //cfg.invs_mean = var[k];
            //cfg.async_return = var[k];
            //cfg.async_release_offset = var[k];           
            //cfg.fork_join_max_seg = (int) (2 * var[k]);
            cfg.e_mean = strand_e * 2400;

//            if (ptu[k] < 1 && max > 1) {
//                max = 1;
//                min = ptu[k] - (max - ptu[k]);
//            }

            Worker w = new Worker(u, max, min, cfg);
            //Worker w = new Worker(u, ptu[k] * 1.2, ptu[k] * 0.8);
            //System.out.println("ptu" + ptu[k] + " worker, util " + u + " created.");
            k++;
        }
    }
}
